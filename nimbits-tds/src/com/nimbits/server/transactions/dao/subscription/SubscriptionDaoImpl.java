/*
 * Copyright (c) 2010 Tonic Solutions LLC.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.server.transactions.dao.subscription;

import com.nimbits.PMF;
import com.nimbits.client.enums.SubscriptionType;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.subscription.Subscription;
import com.nimbits.client.model.subscription.SubscriptionFactory;
import com.nimbits.client.model.user.User;
import com.nimbits.server.orm.SubscriptionEntity;
import com.nimbits.server.subscription.SubscriptionTransactions;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import java.util.Date;
import java.util.List;

/**
 * Created by Benjamin Sautner
 * User: BSautner
 * Date: 1/17/12
 * Time: 4:18 PM
 */
@SuppressWarnings({"unchecked", "unused"})
public class SubscriptionDaoImpl implements SubscriptionTransactions {

    public SubscriptionDaoImpl(final User u) {

    }


    @Override
    public void subscribe(final Entity entity, final Subscription subscription) {
        addOrUpdateSubscription(entity, subscription);
    }

    private static SubscriptionEntity getSubscription(PersistenceManager pm, Entity entity) {
      return getSubscription(pm, entity.getKey());


    }
    private static SubscriptionEntity getSubscription(PersistenceManager pm, String key) {
        try {
            final SubscriptionEntity result = pm.getObjectById(SubscriptionEntity.class, key);
            return result;

        }
        catch (JDOObjectNotFoundException ex) {
            return null;
        }


    }
    private static void addOrUpdateSubscription(final Entity entity, final Subscription subscription)  {

        final PersistenceManager pm = PMF.get().getPersistenceManager();


        try {



            SubscriptionEntity result = getSubscription(pm, entity);
            if (result != null) {
                final Transaction tx = pm.currentTransaction();
                tx.begin();
                result.setNotifyMethod(subscription.getNotifyMethod());
                result.setSubscriptionType(subscription.getSubscriptionType());
                result.setLastSent(subscription.getLastSent());
                result.setMaxRepeat(subscription.getMaxRepeat());
                result.setEnabled(subscription.getEnabled());
                result.setNotifyFormatJson(subscription.getNotifyFormatJson());
                tx.commit();
                //retObj = EntityTransactionFactory.getInstance(user).getEntityByUUID(result.getKey());
                pm.flush();

            }

                else {
                    final SubscriptionEntity s = new SubscriptionEntity(entity, subscription);
                    pm.makePersistent(s);

                }


            }
            finally {
                pm.close();
            }

        }

    public Subscription readSubscription(final Entity entity)  {

        final PersistenceManager pm = PMF.get().getPersistenceManager();

        Subscription retObj = null;
        try {

            SubscriptionEntity result = getSubscription(pm, entity);
            if (result != null) {

                retObj = SubscriptionFactory.createSubscription(result);
            }
            return retObj;
        }
        finally {
            pm.close();
        }

    }



    @Override
    public List<Subscription> getSubscriptionsToPoint(final Point point) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();
        final List<Subscription> results;
        final List<Subscription> retObj;
        try {
            final Query q = pm.newQuery(SubscriptionEntity.class, "subscribedEntity==p && enabled==e");
            q.declareParameters("String p, Boolean e");
            results = (List<Subscription>) q.execute(point.getKey(), true);
            retObj = SubscriptionFactory.createSubscriptions(results);
            return retObj;
        }
        finally {
            pm.close();
        }
    }

    @Override
    public List<Subscription> getSubscriptionsToPointByType(final Point point, final SubscriptionType type) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();
        final List<Subscription> results;
        final List<Subscription> retObj;
        try {
            final Query q = pm.newQuery(SubscriptionEntity.class, "subscribedEntity==p && subscriptionType==t && enabled==e" );
            q.declareParameters("String p, Integer t, Boolean e");
            results = (List<Subscription>) q.execute(point.getKey(), type.getCode(), true);
            retObj = SubscriptionFactory.createSubscriptions(results);
            return retObj;
        }
        finally {
            pm.close();
        }
    }
    @Override
    public void updateSubscriptionLastSent(final Subscription subscription) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            SubscriptionEntity result = getSubscription(pm, subscription.getKey());
            if (result != null) {
                final Transaction tx = pm.currentTransaction();
                tx.begin();

                result.setLastSent(new Date());
                tx.commit();
            }
        } finally {
            pm.close();
        }


    }

    @Override
    public void deleteSubscription(final Entity entity) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();

        final Subscription retObj = null;
        try {
            SubscriptionEntity result = getSubscription(pm, entity);
            if (result != null) {

                pm.deletePersistent(result);
            }

        }
        finally {
            pm.close();
        }

    }

}
