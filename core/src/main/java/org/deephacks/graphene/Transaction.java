package org.deephacks.graphene;

import org.deephacks.graphene.internal.gql.Query;
import org.fusesource.lmdbjni.Cursor;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Transaction {
  private final Stack<Cursor> cursors = new Stack<>();
  private final org.fusesource.lmdbjni.Transaction tx;
  private final Graphene graphene;
  private boolean readOnly;

  Transaction(Graphene graphene, org.fusesource.lmdbjni.Transaction tx, boolean readOnly) {
    this.tx = tx;
    this.graphene = graphene;
    this.readOnly = readOnly;
  }

  org.fusesource.lmdbjni.Transaction getTx() {
    return tx;
  }

  public <E> boolean put(final E entity) {
    return graphene.put(entity);
  }

  public <E> Optional<E> get(Object key, Class<E> entityClass) {
    return graphene.get(key, entityClass);
  }

  public <E> boolean putNoOverwrite(E entity) {
    return graphene.putNoOverwrite(entity);
  }

  public <E> Optional<E> delete(Object key, Class<E> entityClass) throws DeleteConstraintException {
    return graphene.delete(key, entityClass);
  }

  public <E> List<E> selectAll(Class<E> entityClass) {
    return graphene.selectAll(entityClass);
  }

  public void deleteAll(Class<?> entityClass) {
    graphene.deleteAll(entityClass);
  }

  /**
   * Stream instances.
   *
   * @param cls the type which is target for selection
   * @param <E>         instance type
   * @return stream of instances.
   */
  public <E> Stream<E> stream(Class<E> cls) {
    Schema<E> schema = graphene.getSchema(cls);
    final Cursor cursor = graphene.openPrimaryCursor(tx);
    StreamResultSet<E> objects = new StreamResultSet<>(schema, cursor);
    Spliterator<E> spliterator = Spliterators.spliterator(objects.iterator(), Long.MAX_VALUE, Spliterator.CONCURRENT);
    Stream<E> stream = StreamSupport.stream(spliterator, true);
    if (graphene.getTxManager().peek() != null) {
      push(cursor);
    }
    return stream;
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> query(String query, Schema<T> schema) {
    final Cursor cursor = graphene.openPrimaryCursor(tx);
    Query<T> q = Query.parse(query, schema.getGeneratedClass());
    StreamResultSet objects = new StreamResultSet<>(schema, cursor);
    Spliterator spliterator = objects.spliterator();
    try (Stream stream = StreamSupport.stream(spliterator, false)) {
      stream.onClose(cursor::close);
      if (graphene.getTxManager().peek() != null) {
        push(cursor);
      }
      return q.collect(stream);
    }
  }

  public void commit() {
    closeCursors();
    tx.commit();
  }

  public void rollback() {
    closeCursors();
    tx.abort();
  }

  public void push(Cursor cursor) {
    cursors.push(cursor);
  }

  private void closeCursors() {
    for (Cursor cursor : cursors) {
      try {
        cursor.close();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public static interface Transactional {
    void execute(Transaction tx);
  }

}