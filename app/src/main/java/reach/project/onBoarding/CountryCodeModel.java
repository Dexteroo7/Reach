package reach.project.onBoarding;

/**
 * Created by gauravsobti on 28/02/16.
 */
public class CountryCodeModel {

    private String countryCode;
    private String countryName;

    public CountryCodeModel(String countryCode, String countryName) {
        this.countryCode = countryCode;
        this.countryName = countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

}
