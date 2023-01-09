public class TopicMessage extends Message {
	private long timeout;
	private long startTime;
	private String messageTopicHeader;
	private String topicType;

	public TopicMessage(String header, String message) {
		super(message);
		messageTopicHeader = header;
		startTime = System.currentTimeMillis();
		setTopicAndTimeout();
	}

	private void setTopicAndTimeout() {
		String[] headers = messageTopicHeader.split("-");
		topicType = headers[0];
		timeout = Long.parseLong(headers[1]);
		timeout=timeout*60000;
	}

	public String getTopicType() {
		return topicType;
	}

	public boolean timeoutExpired(long currentTime) {
		return currentTime - (startTime + timeout) > 0;
	}
}