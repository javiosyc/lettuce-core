/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.apigenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.lambdaworks.redis.internal.LettuceSets;

/**
 * Create async API based on the templates.
 *
 * @author Mark Paluch
 */
@RunWith(Parameterized.class)
public class CreateAsyncNodeSelectionClusterApi {

    private Set<String> FILTER_METHODS = LettuceSets.unmodifiableSet("shutdown", "debugOom", "debugSegfault", "digest",
            "close", "isOpen", "BaseRedisCommands.reset", "readOnly", "readWrite", "dispatch");

    private CompilationUnitFactory factory;

    @Parameterized.Parameters(name = "Create {0}")
    public static List<Object[]> arguments() {
        List<Object[]> result = new ArrayList<>();

        for (String templateName : Constants.TEMPLATE_NAMES) {
            if (templateName.contains("Transactional") || templateName.contains("Sentinel")) {
                continue;
            }
            result.add(new Object[] { templateName });
        }

        return result;
    }

    /**
     * @param templateName
     */
    public CreateAsyncNodeSelectionClusterApi(String templateName) {

        String targetName = templateName.replace("Commands", "AsyncCommands").replace("Redis", "NodeSelection");
        File templateFile = new File(Constants.TEMPLATES, "com/lambdaworks/redis/api/" + templateName + ".java");
        String targetPackage = "com.lambdaworks.redis.cluster.api.async";

        // todo: remove AutoCloseable from BaseNodeSelectionAsyncCommands
        factory = new CompilationUnitFactory(templateFile, Constants.SOURCES, targetPackage, targetName, commentMutator(),
                methodTypeMutator(), methodFilter(), importSupplier(), null, null);
        factory.keepMethodSignaturesFor(FILTER_METHODS);
    }

    /**
     * Mutate type comment.
     *
     * @return
     */
    protected Function<String, String> commentMutator() {
        return s -> s.replaceAll("\\$\\{intent\\}", "Asynchronous executed commands on a node selection") + "* @generated by "
                + getClass().getName() + "\r\n ";
    }

    /**
     * Mutate type to async result.
     *
     * @return
     */
    protected Predicate<MethodDeclaration> methodFilter() {
        return method -> !CompilationUnitFactory.contains(FILTER_METHODS, method);
    }

    /**
     * Mutate type to async result.
     *
     * @return
     */
    protected Function<MethodDeclaration, Type> methodTypeMutator() {
        return method -> {
            return CompilationUnitFactory.createParametrizedType("AsyncExecutions", method.getType().toString());
        };
    }

    /**
     * Supply additional imports.
     *
     * @return
     */
    protected Supplier<List<String>> importSupplier() {
        return () -> Collections.singletonList("com.lambdaworks.redis.RedisFuture");
    }

    @Test
    public void createInterface() throws Exception {
        factory.createInterface();
    }
}
