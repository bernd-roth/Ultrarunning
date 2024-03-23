package at.co.netconsulting.runningtracker.view;

import android.app.ProgressDialog;
import android.content.Context;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
public class RestAPI {
    private String httpUrl;
    private Context context;
    public RestAPI(Context context, String httpUrl) {
        this.context = context;
        this.httpUrl = httpUrl;
    }
    public void postRequest() {
        ProgressDialog pDialog = new ProgressDialog(this.context);
        pDialog.setMessage("Loading...");
        pDialog.show();
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
            Request.Method.POST,
            this.httpUrl,
            null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }) {
            @Override
            protected Map getParams() {
                Map params = new HashMap();
                params.put("name", "Androidhive");
                params.put("email", "abc@androidhive.info");
                params.put("password", "password123");

                return params;
            }
        };
    }
}
