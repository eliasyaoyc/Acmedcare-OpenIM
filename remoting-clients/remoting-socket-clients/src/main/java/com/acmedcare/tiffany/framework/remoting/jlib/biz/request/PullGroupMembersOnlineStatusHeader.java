package com.acmedcare.tiffany.framework.remoting.jlib.biz.request;

import com.acmedcare.tiffany.framework.remoting.android.core.CFNotNull;
import com.acmedcare.tiffany.framework.remoting.android.core.exception.RemotingCommandException;
import com.acmedcare.tiffany.framework.remoting.android.core.protocol.CommandCustomHeader;
import com.acmedcare.tiffany.framework.remoting.jlib.Constants;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pull Group Members Online Status
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2018-12-05.
 * @since 2.2.0
 */
@Getter
@Setter
@NoArgsConstructor
public class PullGroupMembersOnlineStatusHeader implements CommandCustomHeader {

  @CFNotNull private String namespace = Constants.DEFAULT_NAMESPACE;

  /**
   * 群组编号
   *
   * <p>
   */
  @CFNotNull private String groupId;

  @Builder
  public PullGroupMembersOnlineStatusHeader(String groupId, String namespace) {
    this.groupId = groupId;
    this.namespace = namespace;
  }

  @Override
  public void checkFields() throws RemotingCommandException {}
}
