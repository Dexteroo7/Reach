package reach.project.onBoarding;

/**
 * Created by ashish on 03/03/16.
 */
public final class CountryData {
    public final String isoCode;
    public final String diallingCode;
    public final String countryName;

    public CountryData(final String isoCode, final String diallingCode, final String countryName) {
        this.isoCode = isoCode;
        this.diallingCode = diallingCode;
        this.countryName = countryName;
    }
}
