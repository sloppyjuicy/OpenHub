/*
 *    Copyright 2017 ThirtyDegressRay
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.thirtydegreesray.openhub.mvp.presenter;

import com.thirtydegreesray.openhub.db.AuthUser;
import com.thirtydegreesray.openhub.db.AuthUserDao;
import com.thirtydegreesray.openhub.db.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.mvp.contract.ISplashContract;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Created on 2017/7/12.
 *
 * @author ThirtyDegreesRay
 */

public class SplashPresenter extends ISplashContract.Presenter {

    private final String TAG = "SplashPresenter";

    @Inject
    public SplashPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    public void getUser() {
        AuthUserDao authUserDao = daoSession.getAuthUserDao();
        List<AuthUser> users = authUserDao.loadAll();
        AuthUser selectedUser = null;
        for(AuthUser user : users){
            if(user.getSelected() && !user.isExpired()){
                selectedUser = user;
                break;
            }
        }

        if(selectedUser == null && users.size() > 0){
            for(AuthUser user : users){
                if(user.isExpired()){
                    authUserDao.delete(user);
                }else{
                    selectedUser = user;
                    selectedUser.setSelected(true);
                    authUserDao.update(selectedUser);
                }
            }
        }

        if(selectedUser != null){
            getUserInfo(selectedUser.getAccessToken());
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mView.showMainPage();
//                }
//            }, 1500);
        } else {
            mView.showOAuth2Page();
        }

    }

    @Override
    public void saveAccessToken(String accessToken, String scope, int expireIn) {
        AuthUser authUser = new AuthUser();
        authUser.setSelected(true);
        authUser.setScope(scope);
        authUser.setExpireIn(expireIn);
        authUser.setAuthTime(new Date());
        authUser.setAccessToken(accessToken);
        daoSession.getAuthUserDao().insert(authUser);
    }

    private void getUserInfo(final String accessToken){

        HttpObserver<Object> httpObserver = new HttpObserver<Object>() {
                    @Override
                    public void onError(Throwable error) {
                        mView.showShortToast(error.getMessage());
                    }

                    @Override
                    public void onSuccess(HttpResponse<Object> response) {
                        if(response.isFromNetWork()){
                            mView.showShortToast("From NetWork");
                        }else if(response.isFromCache()){
                            mView.showShortToast("From Cache");
                        }
                    }
                };

        generalRxHttpExecute(new IObservableCreator<Object, Response<Object>>() {
            @Override
            public Observable<Response<Object>> createObservable(boolean forceNetWork) {
                return getAPPSService().getUser(forceNetWork, accessToken);
            }
        }, httpObserver, true);

    }

}