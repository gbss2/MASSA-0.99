package DiverData_Models;

public class Population implements java.io.Serializable{
	private static final long serialVersionUID = 1L;

	private String populationID;
	private String populationName;
	private String geographicOrigin;
	private String country;
	private String coordinatesPopuation;
	
	public String getPopulationID() {
		return populationID;
	}

	public void setPopulationID(String populationID) {
		this.populationID = populationID;
	}

	public String getPopulationName() {
		return populationName;
	}

	public void setPopulationName(String populationName) {
		this.populationName = populationName;
	}

	public String getGeographicOrigin() {
		return geographicOrigin;
	}

	public void setGeographicOrigin(String geographicOrigin) {
		this.geographicOrigin = geographicOrigin;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCoordinatesPopuation() {
		return coordinatesPopuation;
	}

	public void setCoordinatesPopuation(String coordinatesPopuation) {
		this.coordinatesPopuation = coordinatesPopuation;
	}
}