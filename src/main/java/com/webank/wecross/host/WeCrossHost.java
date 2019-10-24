package com.webank.wecross.host;

import com.webank.wecross.network.NetworkManager;
import com.webank.wecross.p2p.P2PMessage;
import com.webank.wecross.p2p.P2PMessageData;
import com.webank.wecross.resource.Path;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.resource.SimpleResource;
import com.webank.wecross.stub.StateRequest;
import com.webank.wecross.stub.StateResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeCrossHost {

    private Logger logger = LoggerFactory.getLogger(WeCrossHost.class);

    private NetworkManager networkManager;
    private PeerManager peerManager;

    public void start() {
        peerManager.start();
        addSimpleResources();
        syncPeerNetworks();
    }

    public Resource getResource(Path path) throws Exception {
        return networkManager.getResource(path);
    }

    public StateResponse getState(StateRequest request) {
        return networkManager.getState(request);
    }

    public P2PMessageData onRestfulPeerMessage(String method, P2PMessage msg) {
        return peerManager.onRestfulPeerMessage(method, msg);
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    public void setPeerManager(PeerManager peerManager) {
        this.peerManager = peerManager;
    }

    public void syncAllState() {}

    public void syncPeerNetworks() {
        // Update peers' resource into networks
        Set<Peer> activePeers = peerManager.getActivePeers();
        networkManager.updateActivePeerNetwork(activePeers);

        // Update active resource back to peerManager
        Set<String> activeResources = networkManager.getAllNetworkStubResourceName(true);
        peerManager.setActiveResources(activeResources);
    }

    private void addSimpleResources() {
        logger.info("Add simple resource");
        try {
            for (int i = 0; i < 1; i++) {
                String name = "networkx.stubx.simple" + i;
                Path path = Path.decode(name);
                Resource resource = new SimpleResource();
                resource.setPath(path);
                networkManager.addResource(resource);
            }
        } catch (Exception e) {
            logger.warn("Add simple resource exception " + e);
        }
    }
}