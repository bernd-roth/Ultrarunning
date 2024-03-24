package at.co.netconsulting.runningtracker.view;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import at.co.netconsulting.runningtracker.pojo.Run;
import timber.log.Timber;

public class RestAPI {
    private final Gson gson;
    private String httpUrl;
    private Context context;
    private RequestQueue queue;

    public RestAPI(Context context, String httpUrl) {
        this.gson = new Gson();
        this.context = context;
        this.httpUrl = httpUrl;
        this.queue = Volley.newRequestQueue(context);
    }

    private JSONObject convertToJson(Run run) {
        String jsonInString = gson.toJson(run);
        try {
            JSONObject jsonObject = new JSONObject(jsonInString);
            Timber.d("DatabaseActivity: exportToServer: %s", jsonInString);
            return jsonObject;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    private JsonObjectRequest asyncEnqueue(JSONObject request, Response.Listener<JSONObject> responseListener, Response.ErrorListener errorListener) {
        return new JsonObjectRequest(
                Request.Method.POST,
                this.httpUrl,
                request,
                responseListener,
                errorListener);
    }

    public void postRequest(Response.ErrorListener errorListener, Iterator<Run> allEntries) {
        if (!allEntries.hasNext())
            return;
        Run run = allEntries.next();
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Timber.d("RestAPI: onResponse: %s", response);
                postRequest(errorListener, allEntries);
            }
        };
        JSONObject jsonObject = convertToJson(run);
        JsonObjectRequest jsonObjReq = asyncEnqueue(jsonObject, responseListener, errorListener);
        queue.add(jsonObjReq);
    }

}
