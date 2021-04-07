package com.example.flutter_braintree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.braintreepayments.api.exceptions.BraintreeError;
import com.braintreepayments.api.exceptions.ErrorWithResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;

public class FlutterBraintreePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, ActivityResultListener {
    private static final int CUSTOM_ACTIVITY_REQUEST_CODE = 0x420;

    private Activity activity;
    private Result activeResult;

    private FlutterBraintreeDropIn dropIn;

    public static void registerWith(Registrar registrar) {
        FlutterBraintreeDropIn.registerWith(registrar);
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_braintree.custom");
        FlutterBraintreePlugin plugin = new FlutterBraintreePlugin();
        plugin.activity = registrar.activity();
        registrar.addActivityResultListener(plugin);
        channel.setMethodCallHandler(plugin);
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        final MethodChannel channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_braintree.custom");
        channel.setMethodCallHandler(this);

        dropIn = new FlutterBraintreeDropIn();
        dropIn.onAttachedToEngine(binding);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        dropIn.onDetachedFromEngine(binding);
        dropIn = null;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
        dropIn.onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
        dropIn.onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
        dropIn.onReattachedToActivityForConfigChanges(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
        dropIn.onDetachedFromActivity();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (activeResult != null) {
            result.error("already_running", "Cannot launch another custom activity while one is already running.", null);
            return;
        }
        activeResult = result;

        if (call.method.equals("tokenizeCreditCard")) {
            String authorization = call.argument("authorization");
            Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
            intent.putExtra("type", "tokenizeCreditCard");
            intent.putExtra("authorization", (String) call.argument("authorization"));

            assert (call.argument("request") instanceof Map);

            Map request = (Map) call.argument("request");
            intent.putExtra("cardNumber", (String) request.get("cardNumber"));
            intent.putExtra("expirationMonth", (String) request.get("expirationMonth"));
            intent.putExtra("expirationYear", (String) request.get("expirationYear"));
            intent.putExtra("shouldValidate", (Boolean) request.get("shouldValidate"));

            if (request.get("cvv") != null) {
                intent.putExtra("cvv", (String) request.get("cvv"));
            }

            activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
        }  else if (call.method.equals("requestDeviceData") || call.method.equals("requestPayPalDeviceData")) {
            String authorization = call.argument("authorization");
            Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
            intent.putExtra("type", call.method);
            intent.putExtra("authorization", (String) call.argument("authorization"));

            activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
        } else if (call.method.equals("requestPaypalNonce")) {
            String authorization = call.argument("authorization");
            Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
            intent.putExtra("type", "requestPaypalNonce");
            intent.putExtra("authorization", (String) call.argument("authorization"));
            assert (call.argument("request") instanceof Map);
            Map request = (Map) call.argument("request");
            intent.putExtra("amount", (String) request.get("amount"));
            intent.putExtra("currencyCode", (String) request.get("currencyCode"));
            intent.putExtra("displayName", (String) request.get("displayName"));
            intent.putExtra("billingAgreementDescription", (String) request.get("billingAgreementDescription"));
            activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
        } else if (call.method.equals("threeDSecure")) {
            String authorization = call.argument("authorization");
            Intent intent = new Intent(activity, FlutterBraintreeCustom.class);
            intent.putExtra("type", "threeDSecure");
            intent.putExtra("authorization", (String) call.argument("authorization"));
            assert (call.argument("request") instanceof Map);
            Map request = (Map) call.argument("request");
            intent.putExtra("amount", (String) request.get("amount"));
            intent.putExtra("nonce", (String) request.get("nonce"));
            intent.putExtra("email", (String) request.get("email"));

            HashMap<String, String> address = (HashMap<String, String>) request.get("address");
            intent.putExtra("address", address);

            activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
        } else if (call.method.equals("canMakePaymentsWithGooglePay")) {
            String authorization = call.argument("authorization");
            Intent intent = new Intent(activity, FlutterBraintreeCustom.class);

            intent.putExtra("type", call.method);
            intent.putExtra("authorization", (String) call.argument("authorization"));

            activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
        } else if (call.method.equals("requestGooglePayPayment")) {
            String authorization = call.argument("authorization");
            Intent intent = new Intent(activity, FlutterBraintreeCustom.class);

            intent.putExtra("type", call.method);
            intent.putExtra("authorization", (String) call.argument("authorization"));

            assert (call.argument("request") instanceof Map);

            Map request = (Map) call.argument("request");
            intent.putExtra("totalPrice", (String) request.get("totalPrice"));
            intent.putExtra("currencyCode", (String) request.get("currencyCode"));
            intent.putExtra("billingAddressRequired", (Boolean) request.get("billingAddressRequired"));

            if (request.get("googleMerchantID") != null) {
                intent.putExtra("googleMerchantID", (String) request.get("googleMerchantID"));
            }

            activity.startActivityForResult(intent, CUSTOM_ACTIVITY_REQUEST_CODE);
        }  else {
            result.notImplemented();
            activeResult = null;
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (activeResult == null)
            return false;

        switch (requestCode) {
            case CUSTOM_ACTIVITY_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    String type = data.getStringExtra("type");
                    if (type.equals("paymentMethodNonce")) {
                        activeResult.success(data.getSerializableExtra("paymentMethodNonce"));
                    } else if (type.equals("deviceDataResponse")) {
                        activeResult.success(data.getSerializableExtra("deviceData"));
                    } else if (type.equals("canMakePaymentsResponse")) {
                        activeResult.success(data.getSerializableExtra("canMakePayments"));
                    } else {
                        Exception error = new Exception("Invalid activity result type.");
                        activeResult.error("error", error.getMessage(), null);
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    activeResult.success(null);
                } else {
                    Exception error = (Exception) data.getSerializableExtra("error");
                    String message = error.getMessage();
                    String details = "";
                    try {
                        ErrorWithResponse errorWithResponse = (ErrorWithResponse) error;
                        List<String> nestedMessages = nestedMessages(errorWithResponse.getFieldErrors());
                        message = errorWithResponse.getLocalizedMessage();
                        if (nestedMessages != null) {
                          details = TextUtils.join(", ", nestedMessages);
                        }
                    } catch (ClassCastException ignored) {}

                    activeResult.error("error", message, details);
                }
                activeResult = null;
                return true;
            default:
                return false;
        }
    }

    List<String> nestedMessages(List<BraintreeError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        List<String> currentMessages = new ArrayList<>();
        for (BraintreeError error: errors) {
            if (error.getMessage() != null) {
                currentMessages.add(error.getMessage());
            }
            List<String> nestedResult = nestedMessages(error.getFieldErrors());
            if (nestedResult != null) {
                currentMessages.addAll(nestedResult);
            }
        }
        return currentMessages;
    }
}
