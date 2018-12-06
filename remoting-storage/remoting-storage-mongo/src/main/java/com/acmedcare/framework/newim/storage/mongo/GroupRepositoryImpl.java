package com.acmedcare.framework.newim.storage.mongo;

import static com.acmedcare.framework.newim.CommonLogger.mongoLog;
import static com.acmedcare.framework.newim.storage.IMStorageCollections.GROUP;
import static com.acmedcare.framework.newim.storage.IMStorageCollections.REF_GROUP_MEMBER;
import static org.springframework.data.mongodb.SessionSynchronization.ALWAYS;
import static org.springframework.data.mongodb.SessionSynchronization.ON_ACTUAL_TRANSACTION;

import com.acmedcare.framework.newim.Group;
import com.acmedcare.framework.newim.Group.GroupMembers;
import com.acmedcare.framework.newim.GroupMemberRef;
import com.acmedcare.framework.newim.Status;
import com.acmedcare.framework.newim.client.bean.Member;
import com.acmedcare.framework.newim.storage.api.GroupRepository;
import com.acmedcare.framework.newim.storage.exception.StorageException;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Group Repository Impl
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 21/11/2018.
 */
@Component
public class GroupRepositoryImpl implements GroupRepository {
  private final MongoTemplate mongoTemplate;
  private final TransactionTemplate transactionTemplate;
  private final MongoClient mongoClient;

  @Autowired
  public GroupRepositoryImpl(
      MongoTemplate mongoTemplate,
      MongoTransactionManager mongoTransactionManager,
      MongoClient mongoClient,
      TransactionTemplate transactionTemplate) {
    this.mongoTemplate = mongoTemplate;
    this.mongoClient = mongoClient;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public Group queryGroup(String groupId) {
    return mongoTemplate.findOne(
        new Query(Criteria.where("groupId").is(groupId)), Group.class, GROUP);
  }

  @Override
  public void saveGroup(Group group) {
    boolean exist =
        mongoTemplate.exists(new Query(Criteria.where("groupId").is(group.getGroupId())), GROUP);

    if (!exist) {
      mongoTemplate.save(group, GROUP);
    } else {
      mongoLog.warn("群组:{},已经存在,不重复添加", group.getGroupId());
    }
  }

  @Override
  public void updateGroup(Group group) {
    Query query = new Query(Criteria.where("groupId").is(group.getGroupId()));
    Update update = new Update();
    update.set("groupName", group.getGroupName());
    update.set("groupOwner", group.getGroupOwner());
    update.set("groupBizTag", group.getGroupBizTag());
    update.set("groupExt", group.getGroupExt());
    UpdateResult updateResult = this.mongoTemplate.updateFirst(query, update, GROUP);
    long row = updateResult.getModifiedCount();
    mongoLog.info("请求更新群组返回值:{}", row);
  }

  @Override
  public long removeGroup(String groupId) {
    mongoLog.info("请求删除群组:{}", groupId);
    Query query = new Query(Criteria.where("groupId").is(groupId));
    Update update = new Update();
    update.set("status", Status.DISABLED);
    UpdateResult updateResult = this.mongoTemplate.updateFirst(query, update, GROUP);

    // change remove rule ,flag
    /*
    DeleteResult dr1 = mongoTemplate.remove(query, GROUP);
    mongoLog.info("删除群组影响行数:{}", dr1.getDeletedCount());
    DeleteResult dr2 = mongoTemplate.remove(query, REF_GROUP_MEMBER);
    mongoLog.info("删除群组与成员关联关系记录影响行数:{}", dr2.getDeletedCount());
    */

    return updateResult.getModifiedCount();
  }

  @Override
  public void saveGroupMembers(GroupMembers members) {
    // REF_GROUP_MEMBER
    if (members.getMembers() != null && members.getMembers().size() > 0) {

      if (!mongoTemplate.exists(
          new Query(Criteria.where("groupId").is(members.getGroupId())), GROUP)) {
        throw new StorageException("群组:" + members.getGroupId() + "不存在");
      }

      List<Member> memberList = members.getMembers();
      List<String> memberIds = Lists.newArrayList();
      for (Member member : memberList) {
        memberIds.add(member.getMemberId().toString());
      }

      Query query =
          new Query(
              Criteria.where("groupId").is(members.getGroupId()).and("memberId").in(memberIds));

      mongoTemplate.setSessionSynchronization(ALWAYS);
      transactionTemplate.execute(
          new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
              try {

                DeleteResult deleteResult = mongoTemplate.remove(query, REF_GROUP_MEMBER);
                mongoLog.info("预删除行数:{} ", deleteResult.getDeletedCount());
                List<GroupMemberRef> refs = new ArrayList<>();
                members
                    .getMembers()
                    .forEach(
                        member ->
                            refs.add(
                                GroupMemberRef.builder()
                                    .groupId(members.getGroupId())
                                    .memberId(member.getMemberId().toString())
                                    .memberName(member.getMemberName())
                                    .build()));

                mongoTemplate.insert(refs, REF_GROUP_MEMBER);
              } catch (Exception e) {
                mongoLog.error("添加群组成员方法异常回滚", e);
                transactionStatus.setRollbackOnly();
              } finally {
                mongoTemplate.setSessionSynchronization(ON_ACTUAL_TRANSACTION);
              }
            }
          });
    }
  }

  @Override
  public long removeGroupMembers(String groupId, List<String> memberIds) {

    mongoLog.info("请求删除群组:{},成员列表:{}", groupId, Arrays.toString(memberIds.toArray()));
    Query query = new Query(Criteria.where("groupId").is(groupId).and("memberId").in(memberIds));
    DeleteResult deleteResult = mongoTemplate.remove(query, REF_GROUP_MEMBER);
    mongoLog.info("删除群组成员影响行数:{}", deleteResult.getDeletedCount());
    return deleteResult.getDeletedCount();
  }

  @Override
  public List<String> queryGroupMemberIds(String groupId) {
    Query query = new Query(Criteria.where("groupId").is(groupId));
    return mongoTemplate.findDistinct(query, "memberId", REF_GROUP_MEMBER, String.class);
  }

  @Override
  public List<GroupMemberRef> queryGroupMembers(String groupId) {
    Query query = new Query(Criteria.where("groupId").is(groupId));
    return mongoTemplate.find(query, GroupMemberRef.class, REF_GROUP_MEMBER);
  }

  @Override
  public List<Group> queryMemberGroups(String passportId) {

    Query groupIdsQuery = new Query(Criteria.where("memberId").is(passportId));
    List<String> groupIds =
        this.mongoTemplate.findDistinct(groupIdsQuery, "groupId", REF_GROUP_MEMBER, String.class);
    if (!groupIds.isEmpty()) {
      Query groupDetailQuery = new Query(Criteria.where("groupId").in(groupIds));
      return this.mongoTemplate.find(groupDetailQuery, Group.class, GROUP);
    }
    return Lists.newArrayList();
  }
}
