package com.acmedcare.framework.newim.client.bean.request;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link NewGroupRequest}
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 21/11/2018.
 */
@Getter
@Setter
public class NewGroupRequest implements Serializable {

  private static final long serialVersionUID = 4499697628571057249L;

  private String groupId;
  private String groupName;
  private String groupOwner;
  private List<String> memberIds;
}
