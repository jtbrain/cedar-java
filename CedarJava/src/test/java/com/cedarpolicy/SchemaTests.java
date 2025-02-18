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
import java.util.ArrayList;

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
    public void getSchemaEntitiesCedarSchemaTests() {
        Schema schema = new Schema("""
                entity User = {
                    name: String,
                    age?: Long,
                };
                entity Photo in Album;
                entity Album;
                action view
                    appliesTo { principal: [User], resource: [Album] };
                """);

        Iterable<EntityTypeName> principalsIterable = assertDoesNotThrow(() -> {
            return schema.principals();
        });

        ArrayList<EntityTypeName> principals = new ArrayList();
        principalsIterable.forEach(principals::add);

        assertEquals(principals.size(), 1);
        EntityTypeName expectedPrincipal = EntityTypeName.parse("User").get();
        assertEquals(expectedPrincipal, principals.get(0));

        // verify resources
        Iterable<EntityTypeName> resourcesIterable = assertDoesNotThrow(() -> {
            return schema.resources();
        });

        ArrayList<EntityTypeName> resources = new ArrayList();
        resourcesIterable.forEach(resources::add);

        assertEquals(resources.size(), 1);
        EntityTypeName expectedResource = EntityTypeName.parse("Album").get();
        assertEquals(expectedResource, resources.get(0));

        // verify principals for the "view" action
        EntityTypeName actionType = EntityTypeName.parse("Action").get();
        EntityUID action = new EntityUID(actionType, "view");
        Iterable<EntityTypeName> principalsForActionIterable = assertDoesNotThrow(() -> {
            return schema.principalsForAction(action);
        });

        ArrayList<EntityTypeName> principalsForAction = new ArrayList();
        principalsForActionIterable.forEach(principalsForAction::add);

        assertEquals(principalsForAction.size(), 1);
        EntityTypeName expectedPrincipalForAction = EntityTypeName.parse("User").get();
        assertEquals(expectedPrincipalForAction, principalsForAction.get(0));

        // verify resources for the "view" action
        Iterable<EntityTypeName> resourcesForActionIterable = assertDoesNotThrow(() -> {
            return schema.resourcesForAction(action);
        });

        ArrayList<EntityTypeName> resourcesForAction = new ArrayList();
        resourcesForActionIterable.forEach(resourcesForAction::add);

        assertEquals(resourcesForAction.size(), 1);
        EntityTypeName expectedResourcesForAction = EntityTypeName.parse("Album").get();
        assertEquals(expectedResourcesForAction, resourcesForAction.get(0));

        // verify the entity types
        Iterable<EntityTypeName> entityTypesIterable = assertDoesNotThrow(() -> {
            return schema.entityTypes();
        });

        ArrayList<EntityTypeName> entityTypes = new ArrayList();
        entityTypesIterable.forEach(entityTypes::add);

        assertEquals(3, entityTypes.size());
        EntityTypeName expectedEntityTypeUser = EntityTypeName.parse("User").get();
        EntityTypeName expectedEntityTypeAlbum = EntityTypeName.parse("Album").get();
        EntityTypeName expectedEntityTypePhoto = EntityTypeName.parse("Photo").get();
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeUser));
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeAlbum));
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypePhoto));
    }

    @Test
    public void getSchemaEntitiesForMultiPrincipalCedarSchemaTests() {
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
                action edit
                    appliesTo { principal: [User], resource: [Album] };
                action view
                    appliesTo { principal: [User, Admin], resource: [Album, Photo] };
                """);

            Iterable<EntityTypeName> principalsIterable = assertDoesNotThrow(() -> {
                return multiPrincipalSchema.principals();
            });

            ArrayList<EntityTypeName> principals = new ArrayList();
            principalsIterable.forEach(principals::add);

            assertEquals(principals.size(), 3);
            EntityTypeName expectedPrincipal1 = EntityTypeName.parse("User").get();
            EntityTypeName expectedPrincipal2 = EntityTypeName.parse("Admin").get();
            assertEquals(2, Collections.frequency(principals, expectedPrincipal1));
            assertEquals(1, Collections.frequency(principals, expectedPrincipal2));

            Iterable<EntityTypeName> resourcesIterable = assertDoesNotThrow(() -> {
                return multiPrincipalSchema.resources();
            });

            ArrayList<EntityTypeName> resources = new ArrayList();
            resourcesIterable.forEach(resources::add);

            assertEquals(resources.size(), 3);
            EntityTypeName expectedResource1 = EntityTypeName.parse("Album").get();
            EntityTypeName expectedResource2 = EntityTypeName.parse("Photo").get();
            assertEquals(2, Collections.frequency(resources, expectedResource1));
            assertEquals(1, Collections.frequency(resources, expectedResource2));

            // verify principals for the "view" action
            EntityTypeName actionType = EntityTypeName.parse("Action").get();
            EntityUID action = new EntityUID(actionType, "view");
            Iterable<EntityTypeName> principalsForActionIterable = assertDoesNotThrow(() -> {
                return multiPrincipalSchema.principalsForAction(action);
            });

            ArrayList<EntityTypeName> principalsForAction = new ArrayList();
            principalsForActionIterable.forEach(principalsForAction::add);

            assertEquals(principalsForAction.size(), 2);
            expectedPrincipal1 = EntityTypeName.parse("User").get();
            expectedPrincipal2 = EntityTypeName.parse("Admin").get();
            assertEquals(1, Collections.frequency(principalsForAction, expectedPrincipal1));
            assertEquals(1, Collections.frequency(principalsForAction, expectedPrincipal2));

            // verify resources for the "view" action
            Iterable<EntityTypeName> resourcesForActionIterable = assertDoesNotThrow(() -> {
                return multiPrincipalSchema.resourcesForAction(action);
            });

            ArrayList<EntityTypeName> resourcesForAction = new ArrayList();
            resourcesForActionIterable.forEach(resourcesForAction::add);

            assertEquals(resourcesForAction.size(), 2);
            expectedResource1 = EntityTypeName.parse("Album").get();
            expectedResource2 = EntityTypeName.parse("Photo").get();
            assertEquals(1, Collections.frequency(resourcesForAction, expectedResource1));
            assertEquals(1, Collections.frequency(resourcesForAction, expectedResource2));

            // verify the entity types
            Iterable<EntityTypeName> entityTypesIterable = assertDoesNotThrow(() -> {
                return multiPrincipalSchema.entityTypes();
            });

            ArrayList<EntityTypeName> entityTypes = new ArrayList();
            entityTypesIterable.forEach(entityTypes::add);

            assertEquals(4, entityTypes.size());
            EntityTypeName expectedEntityTypeUser = EntityTypeName.parse("User").get();
            EntityTypeName expectedEntityTypeAlbum = EntityTypeName.parse("Album").get();
            EntityTypeName expectedEntityTypePhoto = EntityTypeName.parse("Photo").get();
            EntityTypeName expectedEntityTypeAdmin = EntityTypeName.parse("Admin").get();
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeUser));
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeAlbum));
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypePhoto));
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeAdmin));
    }

    @Test
    public void getSchemaEntitiesSchemaJsonTests() {
        Schema schema = assertDoesNotThrow(() -> {
            return Schema.parse(JsonOrCedar.Json, """
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
                                    "resourceTypes": ["Album"]
                                }
                            }
                        }
                    }
                }
                """);
            });

            // verify principals
            Iterable<EntityTypeName> principalsIterable = assertDoesNotThrow(() -> {
                return schema.principals();
            });

            ArrayList<EntityTypeName> principals = new ArrayList();
            principalsIterable.forEach(principals::add);

            assertEquals(principals.size(), 1);
            EntityTypeName expectedPrincipal = EntityTypeName.parse("User").get();
            assertEquals(expectedPrincipal, principals.get(0));

            // verify resources
            Iterable<EntityTypeName> resourcesIterable = assertDoesNotThrow(() -> {
                return schema.resources();
            });

            ArrayList<EntityTypeName> resources = new ArrayList();
            resourcesIterable.forEach(resources::add);

            assertEquals(resources.size(), 1);
            EntityTypeName expectedResource = EntityTypeName.parse("Album").get();
            assertEquals(expectedResource, resources.get(0));

            // verify principals for the "view" action
            EntityTypeName actionType = EntityTypeName.parse("Action").get();
            EntityUID action = new EntityUID(actionType, "view");
            Iterable<EntityTypeName> principalsForActionIterable = assertDoesNotThrow(() -> {
                return schema.principalsForAction(action);
            });

            ArrayList<EntityTypeName> principalsForAction = new ArrayList();
            principalsForActionIterable.forEach(principalsForAction::add);

            assertEquals(principalsForAction.size(), 1);
            EntityTypeName expectedPrincipalForAction = EntityTypeName.parse("User").get();
            assertEquals(expectedPrincipalForAction, principalsForAction.get(0));

            // verify resources for the "view" action
            Iterable<EntityTypeName> resourcesForActionIterable = assertDoesNotThrow(() -> {
                return schema.resourcesForAction(action);
            });

            ArrayList<EntityTypeName> resourcesForAction = new ArrayList();
            resourcesForActionIterable.forEach(resourcesForAction::add);

            assertEquals(resourcesForAction.size(), 1);
            EntityTypeName expectedResourcesForAction = EntityTypeName.parse("Album").get();
            assertEquals(expectedResourcesForAction, resourcesForAction.get(0));

            // verify the entity types
            Iterable<EntityTypeName> entityTypesIterable = assertDoesNotThrow(() -> {
                return schema.entityTypes();
            });

            ArrayList<EntityTypeName> entityTypes = new ArrayList();
            entityTypesIterable.forEach(entityTypes::add);

            assertEquals(3, entityTypes.size());
            EntityTypeName expectedEntityTypeUser = EntityTypeName.parse("User").get();
            EntityTypeName expectedEntityTypeAlbum = EntityTypeName.parse("Album").get();
            EntityTypeName expectedEntityTypePhoto = EntityTypeName.parse("Photo").get();
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeUser));
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeAlbum));
            assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypePhoto));
    }

    @Test
    public void getSchemaEntitiesForMultiPrincipalSchemaJsonTests() {
        Schema multiPrincipalSchema = assertDoesNotThrow(() -> {
            return Schema.parse(JsonOrCedar.Json, """
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
                            "Admin": {
                                "shape": {
                                    "type": "Record",
                                    "attributes": {
                                        "id": {
                                            "type": "String",
                                            "required": true
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
                            "edit": {
                                "appliesTo": {
                                    "principalTypes": ["User"],
                                    "resourceTypes": ["Album"]
                                }
                            },
                            "view": {
                                "appliesTo": {
                                    "principalTypes": ["User", "Admin"],
                                    "resourceTypes": ["Album", "Photo"]
                                }
                            }
                        }
                    }
                }
                """);
        });

        Iterable<EntityTypeName> principalsIterable = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.principals();
        });

        ArrayList<EntityTypeName> principals = new ArrayList();
        principalsIterable.forEach(principals::add);

        assertEquals(principals.size(), 3);
        EntityTypeName expectedPrincipal1 = EntityTypeName.parse("User").get();
        EntityTypeName expectedPrincipal2 = EntityTypeName.parse("Admin").get();
        assertEquals(2, Collections.frequency(principals, expectedPrincipal1));
        assertEquals(1, Collections.frequency(principals, expectedPrincipal2));

        Iterable<EntityTypeName> resourcesIterable = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.resources();
        });

        ArrayList<EntityTypeName> resources = new ArrayList();
        resourcesIterable.forEach(resources::add);

        assertEquals(resources.size(), 3);
        EntityTypeName expectedResource1 = EntityTypeName.parse("Album").get();
        EntityTypeName expectedResource2 = EntityTypeName.parse("Photo").get();
        assertEquals(2, Collections.frequency(resources, expectedResource1));
        assertEquals(1, Collections.frequency(resources, expectedResource2));

        // verify principals for the "view" action
        EntityTypeName actionType = EntityTypeName.parse("Action").get();
        EntityUID action = new EntityUID(actionType, "view");
        Iterable<EntityTypeName> principalsForActionIterable = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.principalsForAction(action);
        });

        ArrayList<EntityTypeName> principalsForAction = new ArrayList();
        principalsForActionIterable.forEach(principalsForAction::add);

        assertEquals(principalsForAction.size(), 2);
        expectedPrincipal1 = EntityTypeName.parse("User").get();
        expectedPrincipal2 = EntityTypeName.parse("Admin").get();
        assertEquals(1, Collections.frequency(principalsForAction, expectedPrincipal1));
        assertEquals(1, Collections.frequency(principalsForAction, expectedPrincipal2));

        // verify resources for the "view" action
        Iterable<EntityTypeName> resourcesForActionIterable = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.resourcesForAction(action);
        });

        ArrayList<EntityTypeName> resourcesForAction = new ArrayList();
        resourcesForActionIterable.forEach(resourcesForAction::add);

        assertEquals(resourcesForAction.size(), 2);
        expectedResource1 = EntityTypeName.parse("Album").get();
        expectedResource2 = EntityTypeName.parse("Photo").get();
        assertEquals(1, Collections.frequency(resourcesForAction, expectedResource1));
        assertEquals(1, Collections.frequency(resourcesForAction, expectedResource2));

        // verify the entity types
        Iterable<EntityTypeName> entityTypesIterable = assertDoesNotThrow(() -> {
            return multiPrincipalSchema.entityTypes();
        });

        ArrayList<EntityTypeName> entityTypes = new ArrayList();
        entityTypesIterable.forEach(entityTypes::add);

        assertEquals(4, entityTypes.size());
        EntityTypeName expectedEntityTypeUser = EntityTypeName.parse("User").get();
        EntityTypeName expectedEntityTypeAlbum = EntityTypeName.parse("Album").get();
        EntityTypeName expectedEntityTypePhoto = EntityTypeName.parse("Photo").get();
        EntityTypeName expectedEntityTypeAdmin = EntityTypeName.parse("Admin").get();
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeUser));
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeAlbum));
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypePhoto));
        assertEquals(1, Collections.frequency(entityTypes, expectedEntityTypeAdmin));
    }
}
