/*
 * Copyright Cedar Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cedarpolicy;

import java.util.Collections;
import java.util.List;

import com.cedarpolicy.model.schema.Schema;
import com.cedarpolicy.model.schema.Schema.JsonOrCedar;
import com.cedarpolicy.value.EntityTypeName;
import com.cedarpolicy.value.EntityUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaTests {
    @Test
    public void parseJsonSchema() {
        assertDoesNotThrow(() -> {
            Schema.parse(JsonOrCedar.Json, "{}");
            Schema.parse(JsonOrCedar.Json, """
                    {
                        "Foo::Bar": {
                            "entityTypes": {},
                            "actions": {}
                        }
                    }
                    """);
            Schema.parse(JsonOrCedar.Json, """
                    {
                        "": {
                            "entityTypes": {
                                "User": {
                                    "shape": {
                                        "type": "Record",
                                        "attributes": {
                                            "name": {
                                                "type": "String",
                                                "required": true
                                            },
                                            "age": {
                                                "type": "Long",
                                                "required": false
                                            }
                                        }
                                    }
                                },
                                "Photo": {
                                    "memberOfTypes": [ "Album" ]
                                },
                                "Album": {}
                            },
                            "actions": {
                                "view": {
                                    "appliesTo": {
                                        "principalTypes": ["User"],
                                        "resourceTypes": ["Photo", "Album"]
                                    }
                                }
                            }
                        }
                    }
                    """);
        });
        assertThrows(Exception.class, () -> {
            Schema.parse(JsonOrCedar.Json, "{\"foo\": \"bar\"}");
            Schema.parse(JsonOrCedar.Json, "namespace Foo::Bar;");
        });
    }

    @Test
    public void parseCedarSchema() {
        assertDoesNotThrow(() -> {
            Schema.parse(JsonOrCedar.Cedar, "");
            Schema.parse(JsonOrCedar.Cedar, "namespace Foo::Bar {}");
            Schema.parse(JsonOrCedar.Cedar, """
                    entity User = {
                        name: String,
                        age?: Long,
                    };
                    entity Photo in Album;
                    entity Album;
                    action view
                      appliesTo { principal: [User], resource: [Album, Photo] };
                    """);
        });
        assertThrows(Exception.class, () -> {
            Schema.parse(JsonOrCedar.Cedar, """
                    {
                        "Foo::Bar": {
                            "entityTypes" {},
                            "actions": {}
                        }
                    }
                    """);
            Schema.parse(JsonOrCedar.Cedar, "namspace Foo::Bar;");
        });
    }

    @Test
    public void getSchemaActionsCedarSchemaTests() {
        Schema schema = new Schema("""
                entity User = {
                    name: String,
                    age?: Long,
                };
                entity Photo in Album;
                entity Album;
                action specificActionGroup;
                action view
                    in [specificActionGroup]
                    appliesTo { principal: [User], resource: [Album] };
                """);

        // verify the actions
        List<EntityUID> actions = assertDoesNotThrow(() -> {
            return schema.actions();
        });

        assertEquals(2, actions.size());
        EntityTypeName expectedActionType = EntityTypeName.parse("Action").get();
        EntityUID expectedActionView = new EntityUID(expectedActionType, "view");
        EntityUID expectedActionSpecificGroup = new EntityUID(expectedActionType, "specificActionGroup");
        assertEquals(1, Collections.frequency(actions, expectedActionView));
        assertEquals(1, Collections.frequency(actions, expectedActionSpecificGroup));

        // verify the action groups
        List<EntityUID> actionGroups = assertDoesNotThrow(() -> {
            return schema.actionGroups();
        });

        assertEquals(1, actionGroups.size());
        assertEquals(expectedActionSpecificGroup, actionGroups.get(0));

        Schema multiPrincipalSchema = new Schema("""
                entity User = {
                    name: String,
                    age?: Long,
                };
                entity Admin = {
                    id: String,
                };
                entity Photo in Album;
                entity Album;
                action specificActionGroup;
                action allActionGroup;
                action edit
                    in [allActionGroup]
                    appliesTo { principal: [User], resource: [Album] };
                action view
                    in [specificActionGroup, allActionGroup]
                    appliesTo { principal: [User, Admin], resource: [Album, Photo] };
                """);

        // verify the actions
        actions = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.actions();
        });

        assertEquals(4, actions.size());
        EntityUID expectedActionEdit = new EntityUID(expectedActionType, "edit");
        EntityUID expectedActionAllActionGroup = new EntityUID(expectedActionType, "allActionGroup");
        assertEquals(1, Collections.frequency(actions, expectedActionView));
        assertEquals(1, Collections.frequency(actions, expectedActionEdit));
        assertEquals(1, Collections.frequency(actions, expectedActionSpecificGroup));
        assertEquals(1, Collections.frequency(actions, expectedActionAllActionGroup));

        // verify the action groups
        actionGroups = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.actionGroups();
        });

        assertEquals(2, actionGroups.size());
        assertEquals(1, Collections.frequency(actionGroups, expectedActionSpecificGroup));
        assertEquals(1, Collections.frequency(actionGroups, expectedActionAllActionGroup));
    }
}
