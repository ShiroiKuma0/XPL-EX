package eu.faircode.xlua.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import eu.faircode.xlua.api.XProxyContent;
import eu.faircode.xlua.api.standard.CallCommandHandler;
import eu.faircode.xlua.api.standard.command.CallPacket;
import eu.faircode.xlua.api.useragent.MockUserAgent;
import eu.faircode.xlua.api.useragent.MockUserAgentDatabaseManager;
import eu.faircode.xlua.utilities.BundleUtil;

public class GetRandomUserAgentCommand extends CallCommandHandler {
    @SuppressWarnings("unused")
    public GetRandomUserAgentCommand() {
        name = "getRandomUserAgent";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        return MockUserAgentDatabaseManager.getRandomUserAgent(
                commandData.getContext(),
                commandData.getDatabase(),
                commandData.getExtraString(MockUserAgent.Table.FIELD_DEVICE, MockUserAgent.GET_UA_ANDROID)).toBundle();
    }

    public static Bundle invoke(Context context, String name) {
        return XProxyContent.mockCall(
                context,
                "getRandomUserAgent",
                BundleUtil.createSingleString(MockUserAgent.Table.FIELD_DEVICE, name));
    }
}
