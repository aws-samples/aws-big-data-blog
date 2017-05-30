package com.amazonaws.services.lambda.model;

public class Partition {

    public static String NAME = "IngestDateTime";

    private final String name;
    private final String spec;
    private final String path;



    public Partition(String spec, String path) {
        this(spec, path, NAME);
    }

    public Partition(String spec, String path, String name){
        this.spec = spec;
        this.path = path;
        this.name = name;
    }

    public String name() { return name; }

    public String spec() {
        return spec;
    }

    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Partition partition = (Partition) o;

        if (!spec.equals(partition.spec)) return false;
        return path.equals(partition.path);

    }

    @Override
    public int hashCode() {
        int result = spec.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
