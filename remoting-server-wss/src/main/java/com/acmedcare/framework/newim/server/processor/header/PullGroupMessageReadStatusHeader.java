package com.acmedcare.framework.newim.server.processor.header;

import com.acmedcare.framework.newim.client.MessageConstants;
import com.acmedcare.tiffany.framework.remoting.CommandCustomHeader;
import com.acmedcare.tiffany.framework.remoting.annotation.CFNotNull;
import com.acmedcare.tiffany.framework.remoting.exception.RemotingCommandException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Pull Group Message Read Status Header
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2018-12-05.
 * @since 2.2.0
 */
@Getter
@Setter
public class PullGroupMessageReadStatusHeader implements CommandCustomHeader {
  private String namespace = MessageConstants.DEFAULT_NAMESPACE;
  /**
   * 群组编号
   *
   * <p>
   */
  @CFNotNull private String groupId;

  /**
   * 消息Id , 可理解为最新的消息,那么之前的消息都标识阅读
   *
   * <p>
   */
  @CFNotNull private String messageId;

  @Override
  public void checkFields() throws RemotingCommandException {
    if (StringUtils.isAnyBlank(groupId, messageId)) {
      throw new RemotingCommandException("拉取消息读取状态请求参数[groupId,messageId]不能为空");
    }
  }
}
