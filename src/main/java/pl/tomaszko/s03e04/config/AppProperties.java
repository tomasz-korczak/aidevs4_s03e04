package pl.tomaszko.s03e04.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Http http = new Http();
    private String hubApiKey = "";
    private final Llm llm = new Llm();
    private final Mcp mcp = new Mcp();
    private final Output output = new Output();
    private final Prompts prompts = new Prompts();

    public Http getHttp() {
        return http;
    }

    public String getHubApiKey() {
        return hubApiKey;
    }

    public void setHubApiKey(String hubApiKey) {
        this.hubApiKey = hubApiKey;
    }

    public Llm getLlm() {
        return llm;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public Output getOutput() {
        return output;
    }

    public Prompts getPrompts() {
        return prompts;
    }

    public static class Http {
        private String baseUrl = "http://localhost:8080";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Llm {
        private String model = "nvidia/nemotron-3-ultra-550b-a55b:free";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Mcp {
        private final Files files = new Files();

        public Files getFiles() {
            return files;
        }

        public static class Files {
            private String javaCommand = "java";
            private String jarPath;
            private String dataRoot;

            public String getJavaCommand() {
                return javaCommand;
            }

            public void setJavaCommand(String javaCommand) {
                this.javaCommand = javaCommand;
            }

            public String getJarPath() {
                return jarPath;
            }

            public void setJarPath(String jarPath) {
                this.jarPath = jarPath;
            }

            public String getDataRoot() {
                return dataRoot;
            }

            public void setDataRoot(String dataRoot) {
                this.dataRoot = dataRoot;
            }
        }
    }

    public static class Output {
        private int byteMin = 4;
        private int byteMax = 500;

        public int getByteMin() {
            return byteMin;
        }

        public void setByteMin(int byteMin) {
            this.byteMin = byteMin;
        }

        public int getByteMax() {
            return byteMax;
        }

        public void setByteMax(int byteMax) {
            this.byteMax = byteMax;
        }
    }

    public static class Prompts {
        private final PromptRef city = new PromptRef();
        private final PromptRef items = new PromptRef();

        public PromptRef getCity() {
            return city;
        }

        public PromptRef getItems() {
            return items;
        }

        public static class PromptRef {
            private String template;

            public String getTemplate() {
                return template;
            }

            public void setTemplate(String template) {
                this.template = template;
            }
        }
    }
}
