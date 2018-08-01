package DiverData_Models;

public class ReferenceSequence implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String referenceSequenceID;
	private String referenceSequenceName;
	private String chormossome;
	private String cytogeneticBand;
	private String contig;
	private String assm_build_version;
	private String assm_coord_start;
	private String assm_coord_end;
	
	public String getReferenceSequenceID() {
		return referenceSequenceID;
	}

	public void setReferenceSequenceID(String referenceSequenceID) {
		this.referenceSequenceID = referenceSequenceID;
	}

	public String getReferenceSequenceName() {
		return referenceSequenceName;
	}

	public void setReferenceSequenceName(String referenceSequenceName) {
		this.referenceSequenceName = referenceSequenceName;
	}

	public String getChormossome() {
		return chormossome;
	}

	public void setChormossome(String chormossome) {
		this.chormossome = chormossome;
	}

	public String getCytogeneticBand() {
		return cytogeneticBand;
	}

	public void setCytogeneticBand(String cytogeneticBand) {
		this.cytogeneticBand = cytogeneticBand;
	}

	public String getContig() {
		return contig;
	}

	public void setContig(String contig) {
		this.contig = contig;
	}

	public String getAssm_build_version() {
		return assm_build_version;
	}

	public void setAssm_build_version(String assm_build_version) {
		this.assm_build_version = assm_build_version;
	}

	public String getAssm_coord_start() {
		return assm_coord_start;
	}

	public void setAssm_coord_start(String assm_coord_start) {
		this.assm_coord_start = assm_coord_start;
	}

	public String getAssm_coord_end() {
		return assm_coord_end;
	}

	public void setAssm_coord_end(String assm_coord_end) {
		this.assm_coord_end = assm_coord_end;
	}
}