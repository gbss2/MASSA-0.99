package DiverData_Models;

public class Sample implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String sampleID;
	private String sampleCode;
	private String collectionDate;
	private String source;
	private String sampleStrategy;
			
	public String getSampleID() {
		return sampleID;
	}

	public void setSampleID(String sampleID) {
		this.sampleID = sampleID;
	}

	public String getSampleCode() {
		return sampleCode;
	}

	public void setSampleCode(String sampleCode) {
		this.sampleCode = sampleCode;
	}

	public String getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(String collectionDate) {
		this.collectionDate = collectionDate;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSampleStrategy() {
		return sampleStrategy;
	}

	public void setSampleStrategy(String sampleStrategy) {
		this.sampleStrategy = sampleStrategy;
	}
}