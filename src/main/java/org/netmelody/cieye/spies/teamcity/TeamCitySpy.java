package org.netmelody.cieye.spies.teamcity;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.netmelody.cieye.core.domain.Status.UNKNOWN;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.domain.Status;
import org.netmelody.cieye.core.domain.TargetDetail;
import org.netmelody.cieye.core.domain.TargetDigest;
import org.netmelody.cieye.core.domain.TargetDigestGroup;
import org.netmelody.cieye.core.domain.TargetId;
import org.netmelody.cieye.core.observation.CiSpy;
import org.netmelody.cieye.core.observation.Contact;
import org.netmelody.cieye.core.observation.KnownOffendersDirectory;
import org.netmelody.cieye.spies.teamcity.jsondomain.Build;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildType;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildTypeDetail;

import com.google.common.base.Predicate;

public final class TeamCitySpy implements CiSpy {

    private static final boolean BuildType = false;
    private final TeamCityCommunicator communicator;
    private final BuildTypeAnalyser buildTypeAnalyser;

    private final Map<TargetId, BuildType> recognisedBuildTypes = newHashMap();
    
    public TeamCitySpy(String endpoint, KnownOffendersDirectory detective, Contact contact) {
        this.communicator = new TeamCityCommunicator(contact, endpoint);
        this.buildTypeAnalyser = new BuildTypeAnalyser(this.communicator, detective);
    }

    @Override
    public TargetDigestGroup targetsConstituting(Feature feature) {
        try {
        final Collection<BuildType> buildTypes = buildTypesFor(feature);
        final List<TargetDigest> digests = newArrayList();
        
        for (BuildType buildType : buildTypes) {
            //this doesnt work when we change the buildType.id??
            final TargetDigest targetDigest = new TargetDigest(communicator.endpoint() + buildType.href, buildType.webUrl(), buildType.name, UNKNOWN);
            digests.add(targetDigest);
            
            recognisedBuildTypes.put(targetDigest.id(), buildType);
        }
        
        return new TargetDigestGroup(digests);
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    @Override
    public TargetDetail statusOf(final TargetId target) {
        try {
        BuildType buildType = recognisedBuildTypes.get(target);
        if (null == buildType) {
            return null;
        }
        return buildTypeAnalyser.targetFrom(buildType);
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }
    
    @Override
    public boolean takeNoteOf(TargetId target, String note) {
        try {
        if (!recognisedBuildTypes.containsKey(target)) {
            return false;
        }
        
        final BuildTypeDetail buildTypeDetail = communicator.detailsFor(recognisedBuildTypes.get(target));
        final Build lastCompletedBuild = communicator.lastCompletedBuildFor(buildTypeDetail);
        if (null != lastCompletedBuild && Status.BROKEN.equals(lastCompletedBuild.status())) {
            communicator.commentOn(lastCompletedBuild, note);
        }

        return true;
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return false;
    }

    private Collection<BuildType> buildTypesFor(final Feature feature) {
        try {
        if (!communicator.canSpeakFor(feature)) {
            return newArrayList();
        }
        
        final Collection<BuildType> buildTypes = communicator.buildTypes();
        if (feature.name().isEmpty()) {
            return buildTypes;
        }
        Collection<BuildType> whatsInIt= filter(buildTypes, withFeatureName(feature.name()));
        //return buildTypes;
        //Predicate<BuildType> testPred = withFeatureName(buildTypes.get(2));
        int k = whatsInIt.size();
        
        String y = "test";
        return buildTypes;
        //return filter(buildTypes, withFeatureName(feature.name()));
        } catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    private Predicate<BuildType> withFeatureName(final String featureName) {
        try {
        return new Predicate<BuildType>() {
            @Override public boolean apply(BuildType buildType) {
                return buildType.projectName.trim().equals(featureName.trim());
            }
        };
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }
}
