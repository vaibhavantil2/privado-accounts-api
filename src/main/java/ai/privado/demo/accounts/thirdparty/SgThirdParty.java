package ai.privado.demo.accounts.thirdparty;

import com.sendgrid.SendGrid;

public class SgThirdParty {

	public static SendGrid getSgClient() {
		return new SendGrid("Dummy-api-key");
	}
}
