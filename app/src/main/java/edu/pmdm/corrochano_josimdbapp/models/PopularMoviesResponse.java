package edu.pmdm.corrochano_josimdbapp.models;

import java.util.List;

public class PopularMoviesResponse {

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
        private TopMeterTitles topMeterTitles;

        // Getters y Setters
        public TopMeterTitles getTopMeterTitles() {
            return topMeterTitles;
        }

        public void setTopMeterTitles(TopMeterTitles topMeterTitles) {
            this.topMeterTitles = topMeterTitles;
        }

        // Clase Anidada TopMeterTitles
        public static class TopMeterTitles {

            // Atributos
            private List<Edge> edges;

            // Getters y Setters
            public List<Edge> getEdges() {
                return edges;
            }

            public void setEdges(List<Edge> edges) {
                this.edges = edges;
            }

        }
    }

    // Clase Anidada Edge
    public static class Edge {

        // Atributos
        private Node node;

        // Getters y Setters
        public Node getNode() {
            return node;
        }

        public void setNode(Node node) {
            this.node = node;
        }

    }

    // Clase Anidada Node
    public static class Node {

        // Atributos
        private String id;
        private TitleText titleText;
        private PrimaryImage primaryImage;

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

        public PrimaryImage getPrimaryImage() {
            return primaryImage;
        }

        public void setPrimaryImage(PrimaryImage primaryImage) {
            this.primaryImage = primaryImage;
        }

    }

    // Clase Anidada TitleText
    public static class TitleText {

        // Atributo
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

        // Atributo
        private String url;

        // Getters y Setters
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }
}
