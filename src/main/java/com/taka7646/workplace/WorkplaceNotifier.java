package com.taka7646.workplace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
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

import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;

public class WorkplaceNotifier extends Notifier {

	private static final Logger logger = Logger.getLogger(WorkplaceNotifier.class.getName());

	private final String url = "";

	private final String notificationStrategy;

	private final String format;

	private final String successMessage;

	private final String failureMessage;

	public String getNotificationStrategy() {
		return notificationStrategy;
	}

	public String getFormat() {
		return format;
	}

	public String getSuccessMessage() {
		return successMessage;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	@DataBoundConstructor
	public WorkplaceNotifier(String notificationStrategy, String format, String successMessage, String failureMessage) {
		this.notificationStrategy = notificationStrategy;
		this.format = format;
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
		EnvVars env = null;
		try {
			env = build.getEnvironment(listener);
		} catch (Exception e) {
			env = new EnvVars();
		}
		String url = env.expand(this.url);
		HttpPost post = new HttpPost(url);
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("username", "vip"));
		post.setEntity(new UrlEncodedFormEntity(params));
		try (CloseableHttpClient client = this.getHttpClient();
				CloseableHttpResponse response = client.execute(post);) {
			logger.info("perform workpalce notify" + response.toString());
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
	}
}
