package com.acmedcare.framework.newim.server.processor;

import com.acmedcare.framework.newim.BizResult;
import com.acmedcare.framework.newim.BizResult.ExceptionWrapper;
import com.acmedcare.framework.newim.server.processor.header.JoinOrLeaveGroupHeader;
import com.acmedcare.framework.newim.server.service.GroupService;
import com.acmedcare.tiffany.framework.remoting.netty.NettyRequestProcessor;
import com.acmedcare.tiffany.framework.remoting.protocol.RemotingCommand;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;

/**
 * Remoting Client Join Or Leave Group Processor
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2018-12-03.
 */
public class RemotingClientJoinOrLeaveGroupProcessor implements NettyRequestProcessor {

  private final GroupService groupService;

  public RemotingClientJoinOrLeaveGroupProcessor(GroupService groupService) {
    this.groupService = groupService;
  }

  @Override
  public RemotingCommand processRequest(
      ChannelHandlerContext channelHandlerContext, RemotingCommand remotingCommand)
      throws Exception {

    RemotingCommand response =
        RemotingCommand.createResponseCommand(remotingCommand.getCode(), null);

    try {

      Object header = remotingCommand.decodeCommandCustomHeader(JoinOrLeaveGroupHeader.class);
      if (header == null) {
        response.setBody(
            BizResult.builder()
                .code(-1)
                .exception(
                    ExceptionWrapper.builder()
                        .message("加群/退群列表请求头参数异常")
                        .type(NullPointerException.class)
                        .build())
                .build()
                .bytes());

        return response;
      }

      JoinOrLeaveGroupHeader joinOrLeaveGroupHeader = (JoinOrLeaveGroupHeader) header;

      switch (joinOrLeaveGroupHeader.getOperateType()) {
        case JOIN:
          this.groupService.joinGroup(
              joinOrLeaveGroupHeader.getGroupId(),
              Lists.newArrayList(joinOrLeaveGroupHeader.getPassportId()));
          break;
        case LEAVE:
          this.groupService.leaveGroup(
              joinOrLeaveGroupHeader.getGroupId(),
              Lists.newArrayList(joinOrLeaveGroupHeader.getPassportId()));
          break;
      }

      response.setBody(BizResult.builder().code(0).build().bytes());

    } catch (Exception e) {
      // exception
      response.setBody(
          BizResult.builder()
              .code(-1)
              .exception(
                  ExceptionWrapper.builder()
                      .message(e.getMessage())
                      .type(e.getCause().getClass())
                      .build())
              .build()
              .bytes());
    }
    return response;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }
}