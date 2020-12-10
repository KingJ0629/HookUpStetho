package com.facebook.stetho.sample;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by Jin on 2020/11/18.
 * Description
 */
public interface GitHubService {
	
	@GET("was/mgr/login")
	Call<String> getData();
}
