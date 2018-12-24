package com.acmedcare.framework.remoting.mq.client.biz.request;

import com.acmedcare.tiffany.framework.remoting.android.core.CFNotNull;
import com.acmedcare.tiffany.framework.remoting.android.core.exception.RemotingCommandException;
import com.acmedcare.tiffany.framework.remoting.android.core.protocol.CommandCustomHeader;
import lombok.Getter;
import lombok.Setter;

/**
 * NewTopicHeader
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2018-12-24.
 */
@Getter
@Setter
public class NewTopicHeader implements CommandCustomHeader {

  private String passport;

  private String passportId;

  /** 主题标识 */
  @CFNotNull private String topicTag;
  /** 主题名称 */
  @CFNotNull private String topicName;

  @Override
  public void checkFields() throws RemotingCommandException {}
}