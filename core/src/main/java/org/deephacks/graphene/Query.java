/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.graphene;

import com.google.common.base.Optional;
import com.sleepycat.je.Cursor;
import org.deephacks.graphene.ResultSet.DefaultResultSet;

public abstract class Query<T> {
    /**
     * Set the position of the first result to retrieve. If this parameter is not
     * set, the query starts from the absolute first instance.
     *
     * @param firstResult beginTransaction position of the first result.
     * @return the same query instance
     */
    public abstract Query<T> setFirstResult(Object firstResult);

    /**
     * Set the position of the last result to retrieve. If this parameter is not
     * set, the query continue until there are no more results or maxResults have been
     * reached.
     *
     * @param firstResult beginTransaction position of the first result.
     * @return the same query instance
     */
    public abstract Query<T> setLastResult(Object firstResult);

    /**
     * Set the maximum number of results to retrieve.
     *
     * @param maxResults max number of instances to fetch (all by default)
     * @return the same query instance
     */
    public abstract Query<T> setMaxResults(int maxResults);

    public abstract ResultSet<T> retrieve();

    static class DefaultQuery<E> extends Query<E> {

        private Object firstResult;
        private Object lastResult;
        private int maxResults = Integer.MAX_VALUE;
        private final Class<E> entityClass;
        private final Optional<Criteria> criteria;
        private final EntityRepository repository;

        DefaultQuery(Class<E> entityClass, EntityRepository repository, Criteria criteria) {
            this.entityClass = entityClass;
            this.criteria = Optional.fromNullable(criteria);
            this.repository = repository;
        }

        DefaultQuery(Class<E> entityClass, EntityRepository repository) {
            this.entityClass = entityClass;
            this.criteria = Optional.absent();
            this.repository = repository;
        }

        @Override
        public Query<E> setFirstResult(Object firstResult) {
            this.firstResult = firstResult;
            return this;
        }

        @Override
        public Query<E> setLastResult(Object lastResult) {
            this.lastResult = lastResult;
            return this;
        }

        @Override
        public Query<E> setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        @Override
        public ResultSet<E> retrieve() {
            Cursor cursor = repository.openPrimaryCursor();
            return new DefaultResultSet<>(entityClass, firstResult, lastResult, maxResults, criteria, cursor);
        }
    }

}