package com.acmedcare.framework.newim.protocol.request;

import com.acmedcare.framework.newim.InstanceNode;
import com.acmedcare.framework.newim.InstanceNode.NodeType;
import com.acmedcare.tiffany.framework.remoting.CommandCustomHeader;
import com.acmedcare.tiffany.framework.remoting.annotation.CFNotNull;
import com.acmedcare.tiffany.framework.remoting.exception.RemotingCommandException;
import lombok.Getter;
import lombok.Setter;

/**
 * Cluster Register Header
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 15/11/2018.
 */
@Getter
@Setter
public class ClusterRegisterHeader implements CommandCustomHeader {

  @CFNotNull private String clusterServerHost;
  @CFNotNull private String clusterReplicaAddress;
  @CFNotNull private boolean hasWssEndpoints = false;

  public InstanceNode instance() {
    return InstanceNode.builder().host(clusterServerHost).nodeType(NodeType.CLUSTER).build();
  }

  public InstanceNode replica() {
    return InstanceNode.builder().host(clusterReplicaAddress).nodeType(NodeType.REPLICA).build();
  }

  @Override
  public void checkFields() throws RemotingCommandException {}
}
