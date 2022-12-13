package com.future.xlink.bean.other;

import java.util.List;

public class Agents {
    public List<String> servers;
    public String timestamp;

    public Agents(List<String> servers, String timestamp) {
        this.servers = servers;
        this.timestamp = timestamp;
    }

    public Agents() {
    }

    @Override
    public String toString() {
        return "Agents{" +
                "servers=" + servers +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
