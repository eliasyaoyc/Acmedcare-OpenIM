package com.acmedcare.framework.newim.server.processor.header;

import com.acmedcare.framework.newim.client.MessageConstants;
import com.acmedcare.tiffany.framework.remoting.CommandCustomHeader;
import com.acmedcare.tiffany.framework.remoting.annotation.CFNotNull;
import com.acmedcare.tiffany.framework.remoting.exception.RemotingCommandException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Pull Group Members
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2018-12-05.
 * @since 2.2.0
 */
@Getter
@Setter
public class PullGroupMembersHeader implements CommandCustomHeader {
  private String namespace = MessageConstants.DEFAULT_NAMESPACE;
  /**
   * 群组编号
   *
   * <p>
   */
  @CFNotNull private String groupId;

  @Override
  public void checkFields() throws RemotingCommandException {
    if (StringUtils.isAnyBlank(groupId)) {
      throw new RemotingCommandException("拉取群组成员列表请求参数[groupId不能为空]");
    }
  }
}
