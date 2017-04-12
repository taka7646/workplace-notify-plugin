package com.taka7646.workplace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;

public class WorkplaceNotifier extends Notifier {

	private static final Logger logger = Logger.getLogger(WorkplaceNotifier.class.getName());

	private final String notificationStrategy;

	private final String format;

	private final String groupId;

	private final String successMessage;

	private final String failureMessage;

	public String getNotificationStrategy() {
		return notificationStrategy;
	}

	public String getFormat() {
		return format;
	}
	
	public String getGroupId(){
		return groupId;
	}

	public String getSuccessMessage() {
		return successMessage;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	@DataBoundConstructor
	public WorkplaceNotifier(String notificationStrategy, String format, String groupId, String successMessage, String failureMessage) {
		this.notificationStrategy = notificationStrategy;
		this.format = format;
		this.groupId = groupId;
		this.successMessage = successMessage;
		this.failureMessage = failureMessage;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		logger.info("perform workpalce notify" + build.getResult().toString());
		NotificationStrategy strategy = NotificationStrategy.fromString(notificationStrategy);
		if (!strategy.needNotification(build)) {
			return true;
		}
		Result result = build.getResult();
		DescriptorImpl desc = (DescriptorImpl)getDescriptor();
		EnvVars env = null;
		try {
			env = build.getEnvironment(listener);
		} catch (Exception e) {
			env = new EnvVars();
		}
		String url = env.expand(desc.getUrlOrDefault()) + String.format("/%s/%s", groupId, "feed");
		HttpPost post = new HttpPost(url);
		
		String message = env.expand(result == Result.SUCCESS? successMessage: failureMessage);
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("access_token", env.expand(desc.getToken())));
		params.add(new BasicNameValuePair("message", message));
		MessageFormat messageFormat = MessageFormat.fromString(format);
		messageFormat.setFormatParam(params);
		post.setHeader("Accept-Charset", "utf-8");
		post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
		try (CloseableHttpClient client = this.getHttpClient();
				CloseableHttpResponse response = client.execute(post);) {
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				listener.getLogger().println("workpalceへ通知を送信しました\n" + message);
			}else{
				listener.getLogger().println("workpalceへ通知に失敗しました\n" + response.toString());
			}
		}
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		logger.info("start workpalce notify" + build.toString());
		return true;
	}

	protected CloseableHttpClient getHttpClient() {
		final HttpClientBuilder clientBuilder = HttpClients.custom();
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

		if (Jenkins.getInstance() != null) {
			ProxyConfiguration proxy = Jenkins.getInstance().proxy;
			if (proxy != null) {
				final HttpHost proxyHost = new HttpHost(proxy.name, proxy.port);
				final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);
				clientBuilder.setRoutePlanner(routePlanner);

				String username = proxy.getUserName();
				String password = proxy.getPassword();
				// Consider it to be passed if username specified. Sufficient?
				if (username != null && !"".equals(username.trim())) {
					logger.info("Using proxy authentication (user=" + username + ")");
					credentialsProvider.setCredentials(new AuthScope(proxyHost),
							new UsernamePasswordCredentials(username, password));
				}
			}
		}
		return clientBuilder.build();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		
		private String token;
		private String url;
		
		public static final NotificationStrategy[] STRATEGIES = NotificationStrategy.values();
		public static final MessageFormat[] FORMATS = MessageFormat.values();
		
		public String getToken(){
			return token;
		}
		
		public String getUrl(){
			return url;
		}

		public String getUrlOrDefault(){
			if(url == null || url.equals("")){
				return "https://graph.facebook.com/v2.8";
			}
			return url;
		}

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Workpalce Notifier";
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			token = formData.getString("token");
			url = formData.getString("url");
			save();
			return super.configure(req, formData);
		}
	}
}
