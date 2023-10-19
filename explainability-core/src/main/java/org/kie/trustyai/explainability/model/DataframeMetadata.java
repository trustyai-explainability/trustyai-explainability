package org.kie.trustyai.explainability.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kie.trustyai.explainability.model.domain.EmptyFeatureDomain;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;

public class DataframeMetadata {
    private final List<String> names;
    private List<String> nameAliases;
    private List<String> cachedColumnNames;
    private List<Type> types;
    private final List<Boolean> constrained;
    private List<FeatureDomain> domains;
    private final List<Boolean> inputs;

    // constructors ====================================================================================================
    public DataframeMetadata() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public DataframeMetadata(List<String> names, List<String> nameAliases, List<Type> types, List<Boolean> constrained, List<FeatureDomain> domains,
            List<Boolean> inputs) {
        this.names = new ArrayList<>(names);
        this.nameAliases = new ArrayList<>(nameAliases);
        this.types = new ArrayList<>(types);
        this.constrained = new ArrayList<>(constrained);
        this.domains = new ArrayList<>(domains);
        this.inputs = new ArrayList<>(inputs);
        updateCachedColumnNames();
    }

    public DataframeMetadata copy() {
        return new DataframeMetadata(
                new ArrayList<>(this.names),
                new ArrayList<>(this.nameAliases),
                new ArrayList<>(this.types),
                new ArrayList<>(this.constrained),
                new ArrayList<>(this.domains),
                new ArrayList<>(this.inputs));
    }

    public void remove(int column) {
        names.remove(column);
        nameAliases.remove(column);
        types.remove(column);
        constrained.remove(column);
        domains.remove(column);
        inputs.remove(column);
        cachedColumnNames.remove(column);
    }

    // row adders ======================================================================================================
    public void add(String name, String nameAlias, Type type, Boolean constraint, FeatureDomain domain, Boolean input) {
        this.names.add(name);
        this.nameAliases.add(nameAlias);
        this.types.add(type);
        this.constrained.add(constraint);
        this.domains.add(domain);
        this.inputs.add(input);
        this.cachedColumnNames.add(nameAlias == null ? name : nameAlias);
    }

    public void add(Feature feature) {
        add(
                feature.getName(),
                null,
                feature.getType(),
                feature.isConstrained(),
                feature.getDomain(),
                true);
    }

    public void add(Output output) {
        add(
                output.getName(),
                null,
                output.getType(),
                true,
                EmptyFeatureDomain.create(),
                false);
    }

    // column name accessors ===========================================================================================
    private void updateCachedColumnNames() {
        List<String> newNames = new ArrayList<>();
        for (int i = 0; i < this.names.size(); i++) {
            if (this.nameAliases.get(i) != null) {
                newNames.add(this.nameAliases.get(i));
            } else {
                newNames.add(this.names.get(i));
            }
        }
        this.cachedColumnNames = newNames;
    }

    protected List<String> getNames() {
        return cachedColumnNames;
    }

    protected String getNames(int i) {
        return cachedColumnNames.get(i);
    }

    protected void setNameAliases(Map<String, String> aliases) {
        List<String> newAliases = new ArrayList<>();
        for (String name : names) {
            if (aliases.containsKey(name)) {
                newAliases.add(aliases.get(name));
            } else {
                newAliases.add(null);
            }
        }
        this.nameAliases = newAliases;
        updateCachedColumnNames();
    }

    // item getters ====================================================================================================
    public String getRawName(int i) {
        return names.get(i);
    }

    public String getNameAlias(int i) {
        return nameAliases.get(i);
    }

    public Type getType(int i) {
        return types.get(i);
    }

    public Boolean getConstrained(int i) {
        return constrained.get(i);
    }

    public FeatureDomain getDomain(int i) {
        return domains.get(i);
    }

    public Boolean getInput(int i) {
        return inputs.get(i);
    }

    // list getters ====================================================================================================
    public List<String> getNameAliases() {
        return nameAliases;
    }

    public List<String> getCachedColumnNames() {
        return cachedColumnNames;
    }

    public List<Type> getTypes() {
        return types;
    }

    public List<Boolean> getConstrained() {
        return constrained;
    }

    public List<FeatureDomain> getDomains() {
        return domains;
    }

    public List<Boolean> getInputs() {
        return inputs;
    }

    //setters ==========================================================================================================
    public void setNameAlias(int i, String nameAlias) {
        this.nameAliases.set(i, nameAlias);
    }

    public void setType(int i, Type type) {
        this.types.set(i, type);
    }

    public void setConstrained(int i, Boolean constraint) {
        this.constrained.set(i, constraint);
    }

    public void setDomain(int i, FeatureDomain domain) {
        this.domains.set(i, domain);
    }

    public void setInput(int i, Boolean input) {
        this.inputs.set(i, input);
    }
}
