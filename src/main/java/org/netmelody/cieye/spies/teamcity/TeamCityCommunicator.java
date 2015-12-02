package org.netmelody.cieye.spies.teamcity;

import static com.google.common.collect.Iterables.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.observation.Contact;
import org.netmelody.cieye.spies.teamcity.jsondomain.Build;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildType;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildTypeDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildTypes;
import org.netmelody.cieye.spies.teamcity.jsondomain.Builds;
import org.netmelody.cieye.spies.teamcity.jsondomain.Change;
import org.netmelody.cieye.spies.teamcity.jsondomain.ChangeDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.Investigation;
import org.netmelody.cieye.spies.teamcity.jsondomain.Investigations;
import org.netmelody.cieye.spies.teamcity.jsondomain.Project;
import org.netmelody.cieye.spies.teamcity.jsondomain.ProjectDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.TeamCityProjects;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public final class TeamCityCommunicator {

    private final Contact contact;
    private final String endpoint;
    private final String prefix;

    public TeamCityCommunicator(Contact contact, String endpoint) {
        this.contact = contact;
        this.endpoint = endpoint;
        this.prefix = (contact.privileged() ? "/httpAuth" : "/guestAuth") + "/app/rest";
    }

    public String endpoint() {
        return this.endpoint;
    }

    public boolean canSpeakFor(Feature feature) {
        return endpoint.equals(feature.endpoint());
    }

    public Collection<Project> projects() {
        try {
        return makeTeamCityRestCall(endpoint + prefix + "/projects", TeamCityProjects.class).project();
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    public Collection<BuildType> buildTypes() {
        try {
            Collection<BuildType> theList = makeTeamCityRestCall(endpoint + prefix + "/buildTypes", BuildTypes.class).buildType();
            Collection<BuildType> theList1 =  new ArrayList<BuildType>();
            
            
            for(BuildType Bt : theList) {
                Collection<Build> builds = buildsPerBuildType((Bt));
                for(Build build : builds) {
                    BuildType toAdd = new BuildType();
                    toAdd.name = build.buildTypeId +" "+build.branchName;
                    toAdd.href = Bt.href;
                    toAdd.projectName = Bt.projectName;
                    toAdd.projectId = Bt.projectId;
                    toAdd.id = Bt.id;
                    
                    
                    boolean contains = theList1.contains(toAdd);
                    
                    theList1.add(toAdd);
                    
                }
            }
            for(BuildType b: theList1) {
                System.out.println(b.name);
            }
        return theList1;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public Collection<Build> buildsPerBuildType(BuildType bt) {
        try {
        //http://dinerotest3.cloudapp.net:3500/app/rest/builds/?locator=running:any,buildType:id:Dinero_Feature,branch:any:default
        Collection<Build> builds = makeTeamCityRestCall(endpoint + prefix +"/builds/?locator=running:any,buildType:id:"+bt.id+",branch:any:default", Builds.class).build();
        Collection<Build> buildsToReturn = new ArrayList<Build>();
        Set<Build> set = new HashSet<Build>();
        set.addAll(builds);
        buildsToReturn.addAll(set);
        
        return buildsToReturn;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Collection<BuildType> buildTypesFor(Project projectDigest) {
        try {
        Collection<BuildType> buildTypes = makeTeamCityRestCall(endpoint + projectDigest.href, ProjectDetail.class).buildTypes.buildType();
        return buildTypes;
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    public BuildTypeDetail detailsFor(BuildType buildType) {
        return makeTeamCityRestCall(endpoint + buildType.href, BuildTypeDetail.class);
    }

    public Build lastCompletedBuildFor(BuildTypeDetail buildTypeDetail) {
        try {
        final Builds completedBuilds = makeTeamCityRestCall(endpoint + buildTypeDetail.builds.href, Builds.class);
        if (null == completedBuilds.build() || completedBuilds.build().isEmpty()) {
            return null;
        }
        return find(completedBuilds.build(), primaryBranchBuilds);
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    public List<Build> runningBuildsFor(BuildType buildType) {
        try {
            return makeTeamCityRestCall(endpoint + prefix + "/builds/?locator=running:true,buildType:id:" + buildType.id, Builds.class).build();
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
        
    }

    public List<Investigation> investigationsOf(BuildType buildType) {
        try {
            return makeTeamCityRestCall(endpoint + buildType.href + "/investigations", Investigations.class).investigation();
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
        
    }

    public BuildDetail detailsOf(Build build) {
        try {
            return makeTeamCityRestCall(endpoint + build.href, BuildDetail.class);   
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    public void commentOn(Build lastCompletedBuild, String note) {
        try {
            contact.doPut(endpoint + lastCompletedBuild.href + "/comment", note);
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        
    }

    public List<Change> changesOf(BuildDetail buildDetail) {
        try {
        final JsonElement json = contact.makeJsonRestCall(endpoint + buildDetail.changes.href);
        final JsonElement change = json.isJsonObject() ? json.getAsJsonObject().get("change") : JsonNull.INSTANCE;
        
        if (null == change || !(change.isJsonArray() || change.isJsonObject())) {
            return ImmutableList.of();
        }
        
        final Gson gson = new Gson();
        final List<Change> changes = new ArrayList<Change>();
        final Iterable<JsonElement> changesJson = change.isJsonArray() ? change.getAsJsonArray() : ImmutableList.of(change);
        for (JsonElement jsonElement : changesJson) {
            changes.add(gson.fromJson(jsonElement, Change.class));
        }
        
        return changes;
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    public ChangeDetail detailedChangesOf(Change change) {
        try {
        return makeTeamCityRestCall(endpoint + change.href, ChangeDetail.class);
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
    }

    private <T> T makeTeamCityRestCall(String url, Class<T> type) {
        try {
            return contact.makeJsonRestCall(url, type);
        } catch(Exception e) {
            e.printStackTrace(System.out);
        }
        return null;
        
    }

    private static final Predicate<Build> primaryBranchBuilds = new Predicate<Build>() {
        @Override public boolean apply(Build input) {
        try {
            
                return input.defaultBranch == null || input.defaultBranch;
                
        }catch(Exception e) {
            e.printStackTrace(System.out);
        }
            return false;
        }
        
    };
}
