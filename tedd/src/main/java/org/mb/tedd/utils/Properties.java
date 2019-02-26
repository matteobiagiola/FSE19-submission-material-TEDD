package org.mb.tedd.utils;

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import org.mb.tedd.algorithm.filter.FilterType;
import org.mb.tedd.algorithm.filter.nlp.POSTaggerStrategy;
import org.mb.tedd.algorithm.filter.nlp.StopWordsFinder;
import org.mb.tedd.algorithm.filter.nlp.VerbObjectDetectionStrategy;
import org.mb.tedd.algorithm.refinement.edgeselection.RefinementStrategies;
import org.mb.tedd.parsing.TestCaseFinder;
import org.mb.tedd.statement.LocatorContainerNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Properties {
    public static String home_dir = System.getProperty("user.home");
    public static String file_separator = System.getProperty("file.separator");
    public static String javaHome = System.getProperty("java.home");
    public static String user_dir = System.getProperty("user.dir");
    public static String mvn_package_structure = "src/main/java";
    public static String multi_value_property_separator = ":";
    public static String classpath_separator = ":";
    public static String[] knownWordsSeparators = {" ", " - ", ", "};
    public static String[] tests_order = new String[] {""};
    public static String[] stop_words = new String[] {""};
    private java.util.Properties appProps;
    private String appPropertiesPath;
    /** All fields representing values, inserted via reflection */
    private static Map<String, Field> parameterMap = new HashMap<>();
    /**
     * This exception is used when a non-existent parameter is accessed
     */
    public static class NoSuchParameterException extends Exception {

        private static final long serialVersionUID = 9074828392047742535L;

        public NoSuchParameterException(String key) {
            super("No such property defined: " + key);
        }
    }

    static {
        // need to do it once, to capture all the default values
        reflectMap();
    }

    private final static Logger logger = Logger.getLogger(Properties.class.getName());
    private static Properties ourInstance;

    /**
     * Parameters are fields of the Properties class, annotated with this
     * annotation. The key parameter is used to identify values in property
     * files or on the command line, the group is used in the config file or
     * input plugins to organize parameters, and the description is also
     * displayed there.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Parameter {
        String key();

        String group();

        String description();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IntValue {
        int min() default Integer.MIN_VALUE;

        int max() default Integer.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface LongValue {
        long min() default Long.MIN_VALUE;

        long max() default Long.MAX_VALUE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface DoubleValue {
        double min() default -(Double.MAX_VALUE - 1); // FIXXME: Check

        double max() default Double.MAX_VALUE;
    }

    @Parameter(key = "baseline", group = "baseline", description = "Baseline flag: if this flag is true the check for missing dependencies in the runtime validation is disabled")
    public static boolean BASELINE = false;

    @Parameter(key = "check_final_graph", group = "validation_check", description = "Enable runtime validation algorithm check.")
    public static boolean CHECK_FINAL_GRAPH = false;

    @Parameter(key = "final_graph_path", group = "validation_check", description = "Absolute path of the final graph to be checked. It holds if check_final_graph is true.")
    public static String FINAL_GRAPH_PATH = "";

    @Parameter(key = "execute_whole_test_suite", group = "validation_check", description = "Execute the whole test suite to compare it with the schedule that has the highest runtime.")
    public static boolean EXECUTE_WHOLE_TEST_SUITE = true;

    @Parameter(key = "tests_path", group = "default", description = "Absolute path of tests in file system.")
    public static String TESTS_PATH = "";

    @Parameter(key = "test_case_words_separator", group = "default", description = "Separator for tokenization of the test case name. If empty words are split according to the camel case pattern.")
    public static String TEST_CASE_WORDS_SEPARATOR = "";

    @Parameter(key = "project_name",  group = "default", description = "Name of the project with test suite.")
    public static String PROJECT_NAME = "";

    @Parameter(key = "print_intermediate_graphs",  group = "default", description = "If true all intermediate dependency graphs are printed during the refinement.")
    public static boolean PRINT_INTERMEDIATE_GRAPHS = false;

    @Parameter(key = "test_suite_path", group = "default", description = "Absolute path of test suite in file system.")
    public static String TEST_SUITE_PATH = "";

    @Parameter(key = "ground_truth_check", group = "refine", description = "If enabled the dependency graph in 'graph_path' is the ground truth, therefore all the dependencies in that graph must be either manifest or uninteresting.")
    public static boolean GROUND_TRUTH_CHECK = false;

    @Parameter(key = "page_objects", group = "selenium", description = "If test cases are written using the page object design pattern or not.")
    public static boolean PAGE_OBJECTS = false;

    @Parameter(key = "write_actions", group = "selenium", description = "Write operations mapped to selenium actions separated by ':'.")
    public static String[] WRITE_ACTIONS = new String[] {"sendKeys"};

    @Parameter(key = "dependency_graph_path", group = "algorithm", description = "Absolute path of dependency graph dot file in file system.")
    public static String DEPENDENCY_GRAPH_PATH = "";

    @Parameter(key = "edit_distance_string_analysis", group = "algorithm", description = "Enable or disable edit_distance = 1 when comparing constant string values in string analysis.")
    public static boolean EDIT_DISTANCE_STRING_ANALYSIS = false;

    @Parameter(key = "refinement_strategy", group = "algorithm", description = "Strategy used to select edges in the dependency graph. See RefinementStrategies class for values.")
    public static String REFINEMENT_STRATEGY = RefinementStrategies.Strategy.SOURCE_FIRST.getStrategyName();

    @Parameter(key = "add_missing_deps_manually", group = "algorithm", description = "If true the missing dependencies, if any, have to be added manually by the user.")
    public static boolean ADD_MISSING_DEPS_MANUALLY = true;

    @Parameter(key = "filter_dependencies", group = "algorithm", description = "Enable filtering of dependencies computed by string analysis.")
    public static boolean FILTER_DEPENDENCIES = false;

    @Parameter(key = "values_to_filter", group = "algorithm", description = "Common values to filter during string analysis separated by ':'. Example: admin, password. It works if the FILTER_TYPE property = common_values.")
    public static String[] VALUES_TO_FILTER = new String[]{};

    @Parameter(key = "filter_type", group = "algorithm", description = "Type of filtering used to filter dependencies computed by string analysis. It works if the FILTER_DEPENDENCIES flag is true.")
    public static String FILTER_TYPE = FilterType.Type.NLP.getTypeName();

    @Parameter(key = "pos_tagger_strategy", group = "algorithm", description = "Type of algorithm used to tag elements of the test case identifier name. It works if the FILTER_TYPE = nlp.")
    public static String POS_TAGGER_STRATEGY = POSTaggerStrategy.Strategy.MAX_ENT.getStrategyName();

    @Parameter(key = "verb_position_in_test_case_name", group = "algorithm", description = "Position of the verb in the test case identifier (starting from 0), assuming that there is always a verb in the test case identifier. It is needed to compute precision and recall for the NLP strategies. It works if FILTER_TYPE = nlp.")
    public static int VERB_POSITION_IN_TEST_CASE_NAME = 0;

    @Parameter(key = "verb_object_detection", group = "algorithm", description = "Enable searching for object of a verb in NLP analysis of the test case identifiers. It works if the FILTER_TYPE = nlp.")
    public static boolean VERB_OBJECT_DETECTION = false;

    @Parameter(key = "verb_object_detection_strategy", group = "algorithm", description = "Type of algorithm used to search for direct object of a verb in the test case identifier name. It works if the FILTER_TYPE = nlp.")
    public static String VERB_OBJECT_DETECTION_STRATEGY = VerbObjectDetectionStrategy.Strategy.DEPENDENCY_PARSER.getStrategyName();

    @Parameter(key = "noun_matching", group = "algorithm", description = "Enable searching for nouns in a test case identifier in NLP analysis. It works if the FILTER_TYPE = nlp. It cannot be true if verb_object_detection property is true.")
    public static boolean NOUN_MATCHING = false;

    @Parameter(key = "compute_stop_words", group = "algorithm", description = "Enable computation of stop words based on the test case names in the tests_path. If false stop words are not computed")
    public static boolean COMPUTE_STOP_WORDS = true;

    @Parameter(key = "wordnet_dict_path", group = "algorithm", description = "Absolute path to wordnet dictionary in the file system. It works if FILTER_TYPE = nlp.")
    public static String WORDNET_DICT_PATH = "";

    @Parameter(key = "filter_write_after_write", group = "algorithm", description = "Filter write after write dependencies. It works if FILTER_TYPE = nlp.")
    public static boolean FILTER_WRITE_AFTER_WRITE = false;

    @Parameter(key = "classified_read_verbs", group = "algorithm", description = "Verbs in the test case name that are classified as read actions separated by ':'.")
    public static String[] CLASSIFIED_READ_VERBS = new String[] {""};

    @Parameter(key = "classified_write_verbs", group = "algorithm", description = "Verbs in the test case name that are classified as write actions separated by ':'.")
    public static String[] CLASSIFIED_WRITE_VERBS = new String[] {""};

    @Parameter(key = "recover_missed_dependencies", group = "algorithm", description = "Algorithm to recover missed dependencies after the initial refinement.")
    public static boolean RECOVER_MISSED_DEPENDENCIES = false;

    @Parameter(key = "project_classpath", group = "runtime", description = "Classpath of project with test suite.")
    public static String PROJECT_CLASSPATH = "";

    @Parameter(key = "only_compute_graph_build_time", group = "runtime", description = "If true only the cost of the computing the graph is measured, while the refinement of the graph is not.")
    public static boolean ONLY_COMPUTE_GRAPH_BUILD_TIME = false;

    @Parameter(key = "reset_class_name", group = "runtime", description = "Qualified name of reset class in project with test suite. The algorithm needs to reset the application state and the project with test suite must implement a reset method.")
    public static String RESET_CLASS_NAME = "";

    @Parameter(key = "reset_method_name", group = "runtime", description = "Name of the method of the reset class in project with test suite. The method is going to be called at runtime.")
    public static String RESET_METHOD_NAME = "";


    public void createPropertiesFile() {

        StringBuffer buffer = new StringBuffer();

        Map<String, Set<Parameter>> fieldMap = new HashMap<>();
        for (Field f : Properties.class.getFields()) {
            if (f.isAnnotationPresent(Parameter.class)) {
                Parameter p = f.getAnnotation(Parameter.class);
                if (!fieldMap.containsKey(p.group()))
                    fieldMap.put(p.group(), new HashSet<>());

                fieldMap.get(p.group()).add(p);
            }
        }

        for (String group : fieldMap.keySet()) {

            buffer.append("#--------------------------------------\n");
            buffer.append("# ");
            buffer.append(group);
            buffer.append("\n#--------------------------------------\n\n");
            for (Parameter p : fieldMap.get(group)) {
                buffer.append("# ");
                buffer.append(p.description());
                buffer.append("\n");
                buffer.append(p.key());
                buffer.append("=");
                try {
                    buffer.append(getStringValue(p.key()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                buffer.append("\n\n");
            }
        }

        if(!new File(this.appPropertiesPath).exists()){
            FileUtils.writeFile(buffer.toString(), this.appPropertiesPath);
            logger.debug("Created properties file in " + this.appPropertiesPath + ". Please fill it with proper values.");
            System.exit(0);
        } else {
            FileUtils.writeFile(buffer.toString(), this.appPropertiesPath);
        }


    }

    /**
     * Determine fields that are declared as parameters
     */
    private static void reflectMap() {
        for (Field f : Properties.class.getFields()) {
            if (f.isAnnotationPresent(Parameter.class)) {
                Parameter p = f.getAnnotation(Parameter.class);
                parameterMap.put(p.key(), f);
            }
        }
    }

    private void checkProperties(){
        String testsPath = Properties.TESTS_PATH;
        this.checkFileExistence(testsPath, Properties.TESTS_PATH);

        String testSuitePath = Properties.TEST_SUITE_PATH;
        this.checkFileExistence(testSuitePath, Properties.TEST_SUITE_PATH);

//        String testsOrder = Arrays.stream(Properties.tests_order).collect(Collectors.joining());
//        this.checkPropertyNotEmpty(testsOrder, "tests_order");
//        this.checkMultiValueProperty(Properties.tests_order, multi_value_property_separator, "tests_order");

        String pageObjects = String.valueOf(Properties.PAGE_OBJECTS);
        this.checkPropertyNotEmpty(pageObjects, "page_objects");

        String writeActions = Arrays.stream(Properties.WRITE_ACTIONS).collect(Collectors.joining());
        this.checkPropertyNotEmpty(writeActions, "write_actions");
        this.checkMultiValueProperty(Properties.WRITE_ACTIONS, multi_value_property_separator, "write_actions", false);
        List<String> writeActionNames = Arrays.asList(Properties.WRITE_ACTIONS);
        Optional<String> writeActionOptional
                = writeActionNames.stream().filter(LocatorContainerNames::isLocatorContainer).findAny();
        if(writeActionOptional.isPresent())
            throw new IllegalArgumentException(writeActionOptional.get() + " is a locator container action that by " +
                    "definition is a read action. Cannot be a write action " + writeActionNames);

        String dependencyGraphPath = Properties.DEPENDENCY_GRAPH_PATH;
        this.checkFileExistence(dependencyGraphPath, "dependency_graph_path");

        String classpath = Properties.PROJECT_CLASSPATH;
        this.checkPropertyNotEmpty(classpath, "project_classpath");
        this.checkClasspath(classpath);

        String projectName = Properties.PROJECT_NAME;
        this.checkPropertyNotEmpty(projectName, "project_name");

        String resetClassName = Properties.RESET_CLASS_NAME;
        // TODO check if reset class really exists
        this.checkPropertyNotEmpty(resetClassName, "reset_class_name");

        String resetMethodName = Properties.RESET_METHOD_NAME;
        this.checkPropertyNotEmpty(resetMethodName, "reset_method_name");
        this.checkStaticMethod(resetClassName, testsPath, resetMethodName);

        String refinementStrategy = Properties.REFINEMENT_STRATEGY;
        this.checkPropertyNotEmpty(refinementStrategy, "refinement_strategy");
        if(!RefinementStrategies.isRefinementStrategy(refinementStrategy))
            throw new IllegalArgumentException("Strategy " + refinementStrategy + " is not a refinement strategy. Choose between available values: "
                    + RefinementStrategies.getValues());

        boolean filterDependencies = Properties.FILTER_DEPENDENCIES;
        if(filterDependencies){
            String filterType = Properties.FILTER_TYPE;
            if(!FilterType.isFilterType(filterType))
                throw new IllegalArgumentException("Filter type " + filterType + " is not a filter type. Choose between available values: "
                        + FilterType.getValues());
            this.checkMultiValueProperty(Properties.CLASSIFIED_READ_VERBS, multi_value_property_separator,
                    "classified_read_verbs", false);
            this.checkMultiValueProperty(Properties.CLASSIFIED_WRITE_VERBS, multi_value_property_separator,
                    "classified_write_verbs", false);
            if(filterType.equals(FilterType.Type.NLP.getTypeName())){
                if(Properties.VERB_POSITION_IN_TEST_CASE_NAME < 0)
                    throw new IllegalArgumentException("Value of verb position in test case name property must be >= 0.");
                this.checkFileExistence(Properties.WORDNET_DICT_PATH, "wordnet_dict_path");
            } else if(filterType.equals(FilterType.Type.COMMON_VALUES.getTypeName())){
                if(Properties.VALUES_TO_FILTER.length == 1 && Properties.VALUES_TO_FILTER[0].isEmpty()){
                    // it seems that it the length is always 1 and the string value is empty
                    Properties.VALUES_TO_FILTER = new String[]{};
                }
                this.checkMultiValueProperty(Properties.VALUES_TO_FILTER, multi_value_property_separator,
                        "values_to_filter", true);
            }

            if(Properties.VERB_OBJECT_DETECTION && Properties.NOUN_MATCHING){
                throw new IllegalArgumentException("verb_object_detection and noun_matching cannot be true at the same time.");
            }
        }

    }

    private void checkPropertyNotEmpty(String property, String propertyName){
        if(property.isEmpty())
            throw new IllegalArgumentException("Property " + propertyName + " cannot be empty in " + this.appPropertiesPath);
    }

    private void checkMultiValueProperty(String[] values, String valueSeparator, String propertyName, boolean canBeEmpty){
        List<String> valuesList = Arrays.asList(values);
        if(valuesList.size() == 1 && valuesList.get(0).isEmpty()){ // different also from empty string
            if(!canBeEmpty){
                throw new IllegalArgumentException("Invalid property " + propertyName + "; empty string not valid value");
            }
        } else if(valuesList.size() == 1){
            String property = valuesList.get(0);
            if(!property.contains(valueSeparator)){
                // it maybe a multi-value property that for the moment has a single value
                Pattern pattern = Pattern.compile("[A-Za-z]+"); // check that property only contains characters
                if (!pattern.matcher(property).matches()) {
                    throw new IllegalArgumentException("Invalid property " + propertyName);
                }
            }
        } else if (valuesList.isEmpty() && !canBeEmpty) {
            throw new IllegalArgumentException("Invalid property " + propertyName);
        }
    }

    private void checkFileExistence(String property, String propertyName){
        this.checkPropertyNotEmpty(property, propertyName);
        File filePathFile = new File(property);
        if(!filePathFile.exists())
            throw new IllegalArgumentException("File " + filePathFile
                    + " does not exist in " + this.appPropertiesPath);
    }

    // TODO find classpath pattern
    private void checkClasspath(String classpath){
//        Pattern pattern = Pattern.compile("(\w)");
//        if (!pattern.matcher(classpath).matches()) {
//            throw new IllegalArgumentException("Invalid property " + classpath);
//        }
    }

    // TODO check that resetMethodName is a static method of class resetClassName
    private void checkStaticMethod(String resetClassName, String testsPath, String resetMethodName){

    }

    public String getProperty(String propertyName){
        String value = this.appProps.getProperty(propertyName);
        if(value == null)
            throw new IllegalStateException("getProperty: property with name " + propertyName
                    + " does not exist in " + this.appPropertiesPath);
        else return value;
    }

    public String getProperty(String propertyName, String defaultValue){
        return this.appProps.getProperty(propertyName, defaultValue);
    }

    /**
     * Get class of parameter
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @return a {@link java.lang.Class} object.
     */
     private static Class<?> getType(String key) throws NoSuchParameterException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        return f.getType();
    }

    /**
     * Get description string of parameter
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @return a {@link java.lang.String} object.
     */
    private static String getDescription(String key)
            throws NoSuchParameterException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        Parameter p = f.getAnnotation(Parameter.class);
        return p.description();
    }

    /**
     * Get group name of parameter
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @return a {@link java.lang.String} object.
     */
    private static String getGroup(String key) throws NoSuchParameterException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        Parameter p = f.getAnnotation(Parameter.class);
        return p.group();
    }

    /**
     * Get integer boundaries
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @return a {@link org.mb.tedd.utils.Properties.IntValue} object.
     */
    private static IntValue getIntLimits(String key)
            throws NoSuchParameterException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        return f.getAnnotation(IntValue.class);
    }

    /**
     * Get long boundaries
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @return a {@link org.mb.tedd.utils.Properties.LongValue} object.
     */
    private static LongValue getLongLimits(String key)
            throws NoSuchParameterException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        return f.getAnnotation(LongValue.class);
    }

    /**
     * Get double boundaries
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @return a {@link org.mb.tedd.utils.Properties.DoubleValue} object.
     */
    private static DoubleValue getDoubleLimits(String key)
            throws NoSuchParameterException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        return f.getAnnotation(DoubleValue.class);
    }

    /**
     * Get an integer parameter value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @return a int.
     */
    private static int getIntegerValue(String key)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        return parameterMap.get(key).getInt(null);
    }

    /**
     * Get an integer parameter value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @return a long.
     */
    private static long getLongValue(String key)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        return parameterMap.get(key).getLong(null);
    }

    /**
     * Get a boolean parameter value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @return a boolean.
     */
    private static boolean getBooleanValue(String key)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        return parameterMap.get(key).getBoolean(null);
    }

    /**
     * Get a double parameter value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @return a double.
     */
    private static double getDoubleValue(String key)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        return parameterMap.get(key).getDouble(null);
    }

    /**
     * Get parameter value as string (works for all types)
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @return a {@link java.lang.String} object.
     */
    private static String getStringValue(String key)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        StringBuffer sb = new StringBuffer();
        Object val = parameterMap.get(key).get(null);
        if (val != null && val.getClass().isArray()) {
            int len = Array.getLength(val);
            for (int i = 0; i < len; i++) {
                if (i > 0)
                    sb.append(multi_value_property_separator);

                sb.append(Array.get(val, i));
            }
        } else {
            sb.append(val);
        }
        return sb.toString();
    }

    /**
     * Set parameter to new integer value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a int.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     */
    private void setValue(String key, int value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);

        if (f.isAnnotationPresent(IntValue.class)) {
            IntValue i = f.getAnnotation(IntValue.class);
            if (value < i.min() || value > i.max())
                throw new IllegalArgumentException();
        }

        f.setInt(this, value);
    }

    /**
     * Set parameter to new long value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a long.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     */
    private void setValue(String key, long value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);

        if (f.isAnnotationPresent(LongValue.class)) {
            LongValue i = f.getAnnotation(LongValue.class);
            if (value < i.min() || value > i.max())
                throw new IllegalArgumentException();
        }

        f.setLong(this, value);
    }

    /**
     * Set parameter to new boolean value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a boolean.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     */
    private void setValue(String key, boolean value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        f.setBoolean(this, value);
    }

    /**
     * Set parameter to new double value
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a double.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     */
    private void setValue(String key, double value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key))
            throw new NoSuchParameterException(key);

        Field f = parameterMap.get(key);
        if (f.isAnnotationPresent(DoubleValue.class)) {
            DoubleValue i = f.getAnnotation(DoubleValue.class);
            if (value < i.min() || value > i.max())
                throw new IllegalArgumentException();
        }
        f.setDouble(this, value);
    }

    /**
     * Set parameter to new value from String
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a {@link java.lang.String} object.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setValue(String key, String value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key)) {
            throw new NoSuchParameterException(key);
        }

        Field f = parameterMap.get(key);
        //Enum
        if (f.getType().isEnum()) {
            f.set(null, Enum.valueOf((Class<Enum>) f.getType(),
                    value.toUpperCase()));
        }
        //Integers
        else if (f.getType().equals(int.class)) {
            setValue(key, Integer.parseInt(value));
        } else if (f.getType().equals(Integer.class)) {
            setValue(key, (Integer) Integer.parseInt(value));
        }
        //Long
        else if (f.getType().equals(long.class)) {
            setValue(key, Long.parseLong(value));
        } else if (f.getType().equals(Long.class)) {
            setValue(key, (Long) Long.parseLong(value));
        }
        //Boolean
        else if (f.getType().equals(boolean.class)) {
            setValue(key, strictParseBoolean(value));
        } else if (f.getType().equals(Boolean.class)) {
            setValue(key, (Boolean) strictParseBoolean(value));
        }
        //Double
        else if (f.getType().equals(double.class)) {
            setValue(key, Double.parseDouble(value));
        } else if (f.getType().equals(Double.class)) {
            setValue(key, (Double) Double.parseDouble(value));
        }
        //Array
        else if (f.getType().isArray()) {
            if (f.getType().isAssignableFrom(String[].class)) {
                setValue(key, value.split(multi_value_property_separator));
            }
        } else {
            f.set(null, value);
        }
    }

    /**
     * we need this strict function because Boolean.parseBoolean silently
     * ignores malformed strings
     *
     * @param s string
     * @return boolean
     */
    private boolean strictParseBoolean(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException(
                    "empty string does not represent a valid boolean");
        }

        if (s.equalsIgnoreCase("true")) {
            return true;
        }

        if (s.equalsIgnoreCase("false")) {
            return false;
        }

        throw new IllegalArgumentException(
                "Invalid string representing a boolean: " + s);
    }

    /**
     * <p>
     * setValue
     * </p>
     *
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            an array of {@link java.lang.String} objects.
     * @throws org.mb.tedd.utils.Properties.NoSuchParameterException
     *             if any.
     * @throws java.lang.IllegalArgumentException
     *             if any.
     * @throws java.lang.IllegalAccessException
     *             if any.
     */
    private void setValue(String key, String[] value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key)) {
            throw new NoSuchParameterException(key);
        }

        Field f = parameterMap.get(key);

        f.set(this, value);
    }

    /**
     * Set the given <code>key</code> variable to the given input Object
     * <code>value</code>
     *
     * @param key
     * @param value
     * @throws NoSuchParameterException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private void setValue(String key, Object value)
            throws NoSuchParameterException, IllegalArgumentException,
            IllegalAccessException {
        if (!parameterMap.containsKey(key)) {
            throw new NoSuchParameterException(key);
        }

        Field f = parameterMap.get(key);

        f.set(this, value);
    }




    /**
     * Initialize properties from property file or command line parameters
     */
    private void initializeProperties() throws IllegalStateException{

        for (String parameter : parameterMap.keySet()) {
            try {
                String property = System.getProperty(parameter);
                if (property == null) {
                    property = this.appProps.getProperty(parameter);
                }
                if (property != null) {
                    setValue(parameter, property);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Wrong parameter settings for '" + parameter + "': " + e.getCause());
            }
        }

        // initialize the test_case_order property
        TestCaseFinder testCaseFinder = new TestCaseFinder();
        Properties.tests_order = testCaseFinder.getTestCaseOrder();

        // initialize the stop_words property
        if(Properties.FILTER_TYPE.equals(FilterType.Type.NLP.getTypeName()) && Properties.COMPUTE_STOP_WORDS){
            StopWordsFinder stopWordsFinder = new StopWordsFinder();
            Properties.stop_words = stopWordsFinder.getStopWords(Properties.tests_order);
            logger.info("Stop words: " + Arrays.asList(Properties.stop_words));
        }

    }

    /**
     * Load and initialize a properties file from a given path
     *
     * @param propertiesPath
     *            a {@link java.lang.String} object.
     */
    private void loadProperties(String propertiesPath) {
        loadPropertiesFile(propertiesPath);
        initializeProperties();
        if(new File(propertiesPath).exists()){
            this.checkProperties();
        }
    }

    /**
     * Load a properties file
     *
     * @param propertiesPath
     *            a {@link java.lang.String} object.
     */
    private void loadPropertiesFile(String propertiesPath) {
        this.appProps = new java.util.Properties();
        try {
            InputStream in;
            File propertiesFile = new File(propertiesPath);
            if (propertiesFile.exists()) {
                in = new FileInputStream(propertiesPath);
                this.appProps.load(in);
            } else {
                in = this.getClass().getClassLoader()
                        .getResourceAsStream(propertiesPath);
                if (in != null) {
                    this.appProps.load(in);
                }
            }
        } catch (Exception e) {
            logger.warn("Error: Could not find configuration file "
                    + propertiesPath);
        }
    }

    /** Constructor */
    private Properties(boolean loadProperties) {
        if (loadProperties){
            Path currentRelativePath = Paths.get("");
            String currentDirectoryPath = currentRelativePath.toAbsolutePath().toString();
            this.appPropertiesPath = currentDirectoryPath + "/src/main/resources/app.properties";
            this.loadProperties(appPropertiesPath);
        }

        Path currentRelativePath = Paths.get("");
        String currentDirectoryPath = currentRelativePath.toAbsolutePath().toString();
        String log4jPropertiesFilePath = currentDirectoryPath + "/src/main/resources/log4j.properties";
        if(!new File(log4jPropertiesFilePath).exists())
            throw new RuntimeException("Log4j properties file " + log4jPropertiesFilePath + " does not exist");
    }

    /**
     * Singleton accessor
     *
     * @return a {@link org.mb.tedd.utils.Properties} object.
     */
    public static Properties getInstance() {
        if (ourInstance == null)
            ourInstance = new Properties(true);
        return ourInstance;
    }
}
