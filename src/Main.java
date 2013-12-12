import org.jclouds.compute.ComputeService;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;

import com.google.common.collect.FluentIterable;

import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;

import java.util.Properties;
import java.util.concurrent.TimeUnit;


import static org.jclouds.Constants.*;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.RestContext;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

public class Main {
	//public static String JCLOUD_PROVIDER = "hpcloud-compute";
	private static ComputeService mCompute;
	private static NovaApi mNovaApi;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String tenantName = "", accessKey = "", secretKey = "", zone = "",securityGroup = "";
		if (args.length == 0) {
			System.out.println("Usage: ");
			System.out.println("-t for Tenant name");
			System.out.println("-k for access key");
			System.out.println("-s for secret key");
			System.out.println("-z for zone");
			System.out.println("-g for Security group");
			return;
		}
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-t":
				tenantName = args[i + 1];
				break;
			case "-k":
				accessKey = args[i + 1];
				break;
			case "-s":
				secretKey = args[i + 1];
				break;
			case "-z":
				zone = args[i + 1];
				break;
			case "-g":
				securityGroup = args[i + 1];
				break;
			default:
				break;
			}
		}
		if(tenantName.trim().equals("")){
			System.out.println("Tenant name invalid, use -t for Tenant name");
			return;
		}
		if(accessKey.trim().equals("")){
			System.out.println("Access key invalid, use -k for access key");
			return;
		}
		if(secretKey.trim().equals("")){
			System.out.println("Secret key invalid, use -s for secret key");
			return;
		}
		if(zone.trim().equals("")){
			System.out.println("Zone name invalid, use -z for zone");
			return;
		}
		if(securityGroup.trim().equals("")){
			System.out.println("Security group invalid, use -g for Security group");
			return;
		}
		
		System.out.println("Searching for: " + securityGroup );
		initComputeService(tenantName + ":" + accessKey, secretKey);
		SecurityGroupApi sgApi = mNovaApi
				.getSecurityGroupExtensionForZone(zone).get();
		FluentIterable<? extends SecurityGroup> sgList = sgApi.list();
		String regex = securityGroup+"-[0-9]+";
		for (SecurityGroup sg : sgList){
			if(sg.getName().matches(regex) || sg.getName().equals(securityGroup)){
				System.out.println("Deleting " + sg.getName());
				sgApi.delete(sg.getId());
			}			
		}
		System.out.println("Finished Deleting");
		System.exit(0);
	}

	@SuppressWarnings("rawtypes")
	public static void initComputeService(String identity, String secretKey) {
		Properties properties = new Properties();
		long scriptTimeout = TimeUnit.MILLISECONDS
				.convert(20, TimeUnit.MINUTES);
		properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
		properties.setProperty(PROPERTY_CONNECTION_TIMEOUT, 30000 + "");
		properties.setProperty(PROPERTY_REQUEST_TIMEOUT, 30000 + "");
		properties.setProperty("jclouds.keystone.credential-type",
				"apiAccessKeyCredentials");

		Iterable<Module> modules = ImmutableSet.<Module> of(
				new SLF4JLoggingModule());
		ContextBuilder builder = ContextBuilder.newBuilder("hpcloud-compute")
				.credentials(identity, secretKey).modules(modules)
				.overrides(properties);
		ComputeServiceContext context = builder
				.buildView(ComputeServiceContext.class);

		mCompute = context.getComputeService();
		RestContext mNova = context.unwrap();
		mNovaApi = (NovaApi) mNova.getApi();
	}

}
