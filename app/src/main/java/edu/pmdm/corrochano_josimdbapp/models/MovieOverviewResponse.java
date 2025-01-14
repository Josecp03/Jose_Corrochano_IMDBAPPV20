package edu.pmdm.corrochano_josimdbapp.models;

public class MovieOverviewResponse {

    // Atributos
    private Data data;
    private boolean status;
    private String message;

    // Getters y Setters
    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Clase Anidada Data
    public static class Data {

        // Atributos
        private Title title;

        // Getters y Setters
        public Title getTitle() {
            return title;
        }

        public void setTitle(Title title) {
            this.title = title;
        }

    }

    // Clase Anidada Title
    public static class Title {

        // Atributos
        private String id;
        private TitleText titleText;
        private TitleText originalTitleText;
        private ReleaseYear releaseYear;
        private ReleaseDate releaseDate;
        private TitleType titleType;
        private PrimaryImage primaryImage;
        private RatingsSummary ratingsSummary;
        private EngagementStatistics engagementStatistics;
        private Plot plot;
        private Certificate certificate;
        private Runtime runtime;

        // Getters y Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public TitleText getTitleText() {
            return titleText;
        }

        public void setTitleText(TitleText titleText) {
            this.titleText = titleText;
        }

        public TitleText getOriginalTitleText() {
            return originalTitleText;
        }

        public void setOriginalTitleText(TitleText originalTitleText) {
            this.originalTitleText = originalTitleText;
        }

        public ReleaseYear getReleaseYear() {
            return releaseYear;
        }

        public void setReleaseYear(ReleaseYear releaseYear) {
            this.releaseYear = releaseYear;
        }

        public ReleaseDate getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(ReleaseDate releaseDate) {
            this.releaseDate = releaseDate;
        }

        public TitleType getTitleType() {
            return titleType;
        }

        public void setTitleType(TitleType titleType) {
            this.titleType = titleType;
        }

        public PrimaryImage getPrimaryImage() {
            return primaryImage;
        }

        public void setPrimaryImage(PrimaryImage primaryImage) {
            this.primaryImage = primaryImage;
        }

        public RatingsSummary getRatingsSummary() {
            return ratingsSummary;
        }

        public void setRatingsSummary(RatingsSummary ratingsSummary) {
            this.ratingsSummary = ratingsSummary;
        }

        public EngagementStatistics getEngagementStatistics() {
            return engagementStatistics;
        }

        public void setEngagementStatistics(EngagementStatistics engagementStatistics) {
            this.engagementStatistics = engagementStatistics;
        }

        public Plot getPlot() {
            return plot;
        }

        public void setPlot(Plot plot) {
            this.plot = plot;
        }

        public Certificate getCertificate() {
            return certificate;
        }

        public void setCertificate(Certificate certificate) {
            this.certificate = certificate;
        }

        public Runtime getRuntime() {
            return runtime;
        }

        public void setRuntime(Runtime runtime) {
            this.runtime = runtime;
        }

    }

    // Clase Anidada TitleText
    public static class TitleText {

        // Atributos
        private String text;

        // Getters y Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

    }

    // Clase Anidada ReleaseYear
    public static class ReleaseYear {

        // Atributos
        private int year;
        private Integer endYear;

        // Getters y Setters
        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public Integer getEndYear() {
            return endYear;
        }

        public void setEndYear(Integer endYear) {
            this.endYear = endYear;
        }

    }

    // Clase Anidada ReleaseDate
    public static class ReleaseDate {

        // Atributos
        private int month;
        private int day;
        private int year;

        // Getters y Setters
        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

    }

    // Clase Anidada TitleType
    public static class TitleType {

        // Atributos
        private String text;

        // Getters y Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

    }

    // Clase Anidada PrimaryImage
    public static class PrimaryImage {

        // Atributos
        private String url;

        // Getters y Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    // Clase Anidada RatingsSummary
    public static class RatingsSummary {

        // Atributos
        private double aggregateRating;
        private int voteCount;

        // Getters y Setters
        public double getAggregateRating() {
            return aggregateRating;
        }

        public void setAggregateRating(double aggregateRating) {
            this.aggregateRating = aggregateRating;
        }

        public int getVoteCount() {
            return voteCount;
        }

        public void setVoteCount(int voteCount) {
            this.voteCount = voteCount;
        }

    }

    // Clase Anidada EngagementStatistics
    public static class EngagementStatistics {

        // Atributos
        private WatchlistStatistics watchlistStatistics;

        // Getters y Setters
        public WatchlistStatistics getWatchlistStatistics() {
            return watchlistStatistics;
        }

        public void setWatchlistStatistics(WatchlistStatistics watchlistStatistics) {
            this.watchlistStatistics = watchlistStatistics;
        }

        // Clase Anidada WatchlistStatistics
        public static class WatchlistStatistics {

            // Atributos
            private DisplayableCount displayableCount;

            // Getters y Setters
            public DisplayableCount getDisplayableCount() {
                return displayableCount;
            }

            public void setDisplayableCount(DisplayableCount displayableCount) {
                this.displayableCount = displayableCount;
            }

            // Clase Anidada DisplayableCount
            public static class DisplayableCount {

                // Atributos
                private String text;

                // Getters y Setters
                public String getText() {
                    return text;
                }

                public void setText(String text) {
                    this.text = text;
                }

            }
        }

    }

    // Clase anidada Plot
    public static class Plot {

        // Atributos
        private PlotText plotText;

        // Getters y Setters
        public PlotText getPlotText() {
            return plotText;
        }

        public void setPlotText(PlotText plotText) {
            this.plotText = plotText;
        }

        // Clase anidada PlotText
        public static class PlotText {

            // Atributos
            private String plainText;

            // Getters y Setters
            public String getPlainText() {
                return plainText;
            }

            public void setPlainText(String plainText) {
                this.plainText = plainText;
            }

        }

    }

    // Clase Anidada Certificate
    public static class Certificate {

        // Atributos
        private String rating;

        // Getters y Setters
        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }

    }

    // Clase Anidada RunTime
    public static class Runtime {

        // Atributos
        private int seconds;

        // Getters y Setters
        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }

    }
}
