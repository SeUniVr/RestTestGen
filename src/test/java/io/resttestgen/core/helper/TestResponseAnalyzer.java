package io.resttestgen.core.helper;

import io.resttestgen.core.dictionary.Dictionary;
import org.junit.jupiter.api.Test;

public class TestResponseAnalyzer {

    @Test
    public void testAnalysis() {
        //String jsonString = "[{\"id\":30,\"category\":{\"id\":43,\"name\":null},\"name\":\"null\",\"testBool\":true, \"photoUrls\":[\"prova1\", \"prova2\"],\"tags\":[{\"id\":20,\"name\":null},{\"id\":21,\"name\":\"sHU\"}],\"status\":\"available\"},{\"id\":30,\"category\":{\"id\":33,\"name\":null},\"name\":\"null\",\"testBool\":false, \"seconArray\":[\"prova1\", \"prova2\"],\"tags\":[{\"id\":20,\"name\":null},{\"id\":21,\"name\":\"sHU\"}],\"status\":\"available\"}]";
        //String jsonString = "[1, 2, 3]";
        String jsonString = "[\n" +
                "    {\n" +
                "        \"id\": 1,\n" +
                "        \"category\": {\n" +
                "            \"id\": 1,\n" +
                "            \"name\": \"dogs\"\n" +
                "        },\n" +
                "        \"name\": \"Bau\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"dogphoto.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 1,\n" +
                "                \"name\": \"brown\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 5,\n" +
                "                \"name\": \"big\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"available\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": 2,\n" +
                "        \"category\": {\n" +
                "            \"id\": 2,\n" +
                "            \"name\": \"cats\"\n" +
                "        },\n" +
                "        \"name\": \"Miao\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"catphoto.com\",\n" +
                "            \"catphoto2.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 2,\n" +
                "                \"name\": \"white\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 5,\n" +
                "                \"name\": \"big\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"sold\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": 3,\n" +
                "        \"category\": {\n" +
                "            \"id\": 2,\n" +
                "            \"name\": \"cats\"\n" +
                "        },\n" +
                "        \"name\": \"Kitty\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"kittyphoto.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 4,\n" +
                "                \"name\": \"black\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 5,\n" +
                "                \"name\": \"big\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"pending\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": 4,\n" +
                "        \"category\": {\n" +
                "            \"id\": 3,\n" +
                "            \"name\": \"fishes\"\n" +
                "        },\n" +
                "        \"name\": \"Flipper\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"filpper.com\",\n" +
                "            \"fish.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 1,\n" +
                "                \"name\": \"brown\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 6,\n" +
                "                \"name\": \"medium\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"available\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": 5,\n" +
                "        \"category\": {\n" +
                "            \"id\": 4,\n" +
                "            \"name\": \"birds\"\n" +
                "        },\n" +
                "        \"name\": \"Cip\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"bird.com\",\n" +
                "            \"cip.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 3,\n" +
                "                \"name\": \"gray\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 6,\n" +
                "                \"name\": \"medium\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"available\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": 6,\n" +
                "        \"category\": {\n" +
                "            \"id\": 4,\n" +
                "            \"name\": \"birds\"\n" +
                "        },\n" +
                "        \"name\": \"Ciop\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"ciop.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 4,\n" +
                "                \"name\": \"black\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 7,\n" +
                "                \"name\": \"small\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"available\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"id\": 7,\n" +
                "        \"category\": {\n" +
                "            \"id\": 5,\n" +
                "            \"name\": \"equines\"\n" +
                "        },\n" +
                "        \"name\": \"Noir\",\n" +
                "        \"photoUrls\": [\n" +
                "            \"horsephoto.com\",\n" +
                "            \"noir.com\"\n" +
                "        ],\n" +
                "        \"tags\": [\n" +
                "            {\n" +
                "                \"id\": 2,\n" +
                "                \"name\": \"white\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 4,\n" +
                "                \"name\": \"black\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 6,\n" +
                "                \"name\": \"medium\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"status\": \"sold\"\n" +
                "    }\n" +
                "]";

        Dictionary dictionary = new Dictionary();

        /*ResponseAnalyzer responseAnalyzer = new ResponseAnalyzer(dictionary);
        responseAnalyzer.analyzeResponse(null, jsonString);*/

        // FIXME: finish test case
    }
}
