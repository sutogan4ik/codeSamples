package ru.ibecom.helpers;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.mime.TypedByteArray;
import ru.ibecom.mapmodule.MapDataManager;

/**
 * Created by Prog on 19.05.2015.
 */
public class RetrofitExecutor {
    private Methods methods;
    private ExecutorService pool;
    private final static String PREFIX = "write";
    public RetrofitExecutor(String endPoint, String appId) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(endPoint + appId)
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .build();
        methods = restAdapter.create(Methods.class);
        pool = Executors.newFixedThreadPool(10);
    }

    public void execute(final String pathToSave, final UpdateListener listener){
        pool.submit(new Runnable() {
            @Override
            public void run() {
                Method[] n = methods.getClass().getDeclaredMethods();
                Set<Future<MyResponse>> futures = new HashSet<>();
                for(Method item : n){
                    if(item.getName().contains(PREFIX)) {
                        UpdateCallable callble = new UpdateCallable(item.getName());
                        Future<MyResponse> future = pool.submit(callble);
                        futures.add(future);
                    }
                }
                for(Future<MyResponse> item : futures){
                    Response response;
                    String name;
                    try {
                        response = item.get().response;
                        name = item.get().methodName;
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        name = null;
                        response = null;
                        if (listener != null){
                            listener.someWrong();
                        }
                    }
                    if(name != null && response != null){
                        saveResult(name, response, pathToSave);
                    }
                }
                if(listener != null){
                    listener.updateComplete();
                }
            }
        });
    }

    private void saveResult(String name, Response response, String path){
        int status = response.getStatus();
        if(status == 200) {
            try {
                if(name.equals("writePics")){
                    File zip = new File(path + "/pics.zip");
                    FileOutputStream output = new FileOutputStream(zip);
                    IOUtils.write(((TypedByteArray) response.getBody()).getBytes(), output);
                    DataManager.unpackZip(path, "/pics.zip");
                    zip.delete();
                }else {
                    String answer = getResponseString(response);
                    Method method = MapDataManager.class.getMethod(name, String.class, String.class);
                    method.invoke(null, path, answer);

                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getResponseString(Response response){
        BufferedReader reader;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(response.getBody().in()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return sb.toString();
    }

    private class UpdateCallable implements Callable<MyResponse>{
        private String methodName;

        public UpdateCallable(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public MyResponse call() throws Exception {
            Method method = methods.getClass().getMethod(methodName, null);
            Response response = (Response) method.invoke(methods, null);
            return new MyResponse(methodName, response);
        }

    }

    private class MyResponse{
        public String methodName;
        public Response response;

        public MyResponse(String methodName, Response response) {
            this.methodName = methodName;
            this.response = response;
        }
    }

    private interface Methods{
        @GET("/zones")
        Response writeZones();

        @GET("/canvas")
        Response writeCanvas();

        @GET("/poi")
        Response writePoi();

        @GET("/routes")
        Response writeRoutes();

        @GET("/visualroutes")
        Response writeVisualRoutes();

        @GET("/pics")
        Response writePics();

    }

    public interface UpdateListener{
        void updateComplete();
        void someWrong();
    }
}
