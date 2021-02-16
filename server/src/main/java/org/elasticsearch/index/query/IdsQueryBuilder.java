/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.Query;
import org.elasticsearch.Version;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.IdFieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.elasticsearch.common.xcontent.ObjectParser.fromList;

/**
 * A query that will return only documents matching specific ids (and a type).
 */
public class IdsQueryBuilder extends AbstractQueryBuilder<IdsQueryBuilder> {

    public static final String NAME = "ids";
    private static final ParseField VALUES_FIELD = new ParseField("values");

    private final Set<String> ids = new HashSet<>();

    /**
     * Creates a new IdsQueryBuilder with no types specified upfront
     */
    public IdsQueryBuilder() {
        // nothing to do
    }

    /**
     * Read from a stream.
     */
    public IdsQueryBuilder(StreamInput in) throws IOException {
        super(in);
        if (in.getVersion().before(Version.V_8_0_0)) {
            // types no longer relevant so ignore
            String[] types = in.readStringArray();
            if (types.length > 0) {
                throw new IllegalStateException("types are no longer supported in ids query but found [" + Arrays.toString(types) + "]");
            }
        }
        Collections.addAll(ids, in.readStringArray());
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        if (out.getVersion().before(Version.V_8_0_0)) {
            // types not supported so send an empty array to previous versions
            out.writeStringArray(Strings.EMPTY_ARRAY);
        }
        out.writeStringArray(ids.toArray(new String[ids.size()]));
    }

    /**
     * Adds ids to the query.
     */
    public IdsQueryBuilder addIds(String... ids) {
        if (ids == null) {
            throw new IllegalArgumentException("[" + NAME + "] ids cannot be null");
        }
        Collections.addAll(this.ids, ids);
        return this;
    }

    /**
     * Returns the ids for the query.
     */
    public Set<String> ids() {
        return this.ids;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.startArray(VALUES_FIELD.getPreferredName());
        for (String value : ids) {
            builder.value(value);
        }
        builder.endArray();
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    private static final ObjectParser<IdsQueryBuilder, Void> PARSER = new ObjectParser<>(NAME, IdsQueryBuilder::new);

    static {
        PARSER.declareStringArray(fromList(String.class, IdsQueryBuilder::addIds), IdsQueryBuilder.VALUES_FIELD);
        declareStandardFields(PARSER);
    }

    public static IdsQueryBuilder fromXContent(XContentParser parser) {
        try {
            return PARSER.apply(parser, null);
        } catch (IllegalArgumentException e) {
            throw new ParsingException(parser.getTokenLocation(), e.getMessage(), e);
        }
    }


    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    protected QueryBuilder doRewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        if (ids.isEmpty()) {
            return new MatchNoneQueryBuilder();
        }
        SearchExecutionContext context = queryRewriteContext.convertToSearchExecutionContext();
        if (context != null && context.getFieldType(IdFieldMapper.NAME) == null) {
            // no mappings yet
            return new MatchNoneQueryBuilder();
        }
        return super.doRewrite(queryRewriteContext);
    }

    @Override
    protected Query doToQuery(SearchExecutionContext context) throws IOException {
        MappedFieldType idField = context.getFieldType(IdFieldMapper.NAME);
        if (idField == null || ids.isEmpty()) {
            throw new IllegalStateException("Rewrite first");
        }
        return idField.termsQuery(new ArrayList<>(ids), context);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(ids);
    }

    @Override
    protected boolean doEquals(IdsQueryBuilder other) {
        return Objects.equals(ids, other.ids);
    }
}
