/**
 * 
 */
package com.revealprecision.revealserver.api.v1.facade.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuel Githengi created on 11/18/20
 */
public  class TaskSyncRequestWrapper {
	
	@JsonProperty
	private List<String> plan = new ArrayList<>();
	
	@JsonProperty
	private List<String> group = new ArrayList<>();
	
	@JsonProperty
	private long serverVersion;

	@JsonProperty
	private String owner;
	
	@JsonProperty("return_count")
	private boolean returnCount;

	public List<String> getPlan() {
		return plan;
	}
	
	public List<String> getGroup() {
		return group;
	}
	
	public long getServerVersion() {
		return serverVersion;
	}

	public String getOwner() {
		return owner;
	}

	
	public boolean isReturnCount() {
		return returnCount;
	}
	
}
