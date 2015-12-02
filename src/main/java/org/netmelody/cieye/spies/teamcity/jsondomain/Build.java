package org.netmelody.cieye.spies.teamcity.jsondomain;

import java.util.Objects;

import org.netmelody.cieye.core.domain.Status;

public final class Build {
    public long id;
    public String number;
    public String status;
    public String buildTypeId;
    public String href;
    public String webUrl;
    public boolean running;
    public int percentageComplete;
    public String branchName;
    public Boolean defaultBranch;

    public Status status() {
        if (status == null || "SUCCESS".equals(status)) {
            return Status.GREEN;
        }
        return Status.BROKEN;
    }
    @Override
    public String toString() {
        return branchName;
        
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Build other = (Build) obj;
        if (!Objects.equals(this.branchName, other.branchName)) {
            return false;
        }
        return true;
    }
    public String getName() {
        return branchName;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.branchName);
        return hash;
    }
}
