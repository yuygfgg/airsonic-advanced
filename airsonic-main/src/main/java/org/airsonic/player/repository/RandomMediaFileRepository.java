/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2023 (C) Y.Tory
 */
package org.airsonic.player.repository;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import javax.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RandomMediaFileRepository {

    @Autowired
    private EntityManager entityManager;

    public List<MediaFile> getRandomMediaFiles(String username, RandomSearchCriteria criteria, List<Integer> mediaFileIds) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MediaFile> query = cb.createQuery(MediaFile.class);
        Root<MediaFile> mediaFile = query.from(MediaFile.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isNull(mediaFile.get("indexPath")));
        predicates.add(cb.in(mediaFile.get("id")).value(mediaFileIds));

        addConditionalPredicates(criteria, cb, mediaFile, predicates);

        query.where(predicates.toArray(new Predicate[predicates.size()]));
        query.select(mediaFile);

        TypedQuery<MediaFile> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    private void addConditionalPredicates(RandomSearchCriteria criteria, CriteriaBuilder cb, Root<MediaFile> mediaFile,
            List<Predicate> predicates) {

        if (criteria.getGenre() != null) {
            predicates.add(cb.equal(mediaFile.get("genre"), criteria.getGenre()));
        }

        if (criteria.getFormat() != null) {
            predicates.add(cb.equal(mediaFile.get("format"), criteria.getFormat()));
        }

        if (criteria.getFromYear() != null) {
            predicates.add(cb.greaterThanOrEqualTo(mediaFile.get("year"), criteria.getFromYear()));
        }

        if (criteria.getToYear() != null) {
            predicates.add(cb.lessThanOrEqualTo(mediaFile.get("year"), criteria.getToYear()));
        }

        if (criteria.getMinLastPlayedDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(mediaFile.get("lastPlayed"), criteria.getMinLastPlayedDate()));
        }

        if (criteria.getMaxLastPlayedDate() != null) {
            if (criteria.getMinLastPlayedDate() == null) {
                predicates.add(cb.or(cb.isNull(mediaFile.get("lastPlayed")),
                        cb.lessThanOrEqualTo(mediaFile.get("lastPlayed"), criteria.getMaxLastPlayedDate())));
            } else {
                predicates.add(cb.lessThanOrEqualTo(mediaFile.get("lastPlayed"), criteria.getMaxLastPlayedDate()));
            }
        }

        if (criteria.getMinPlayCount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(mediaFile.get("playCount"), criteria.getMinPlayCount()));
        }

        if (criteria.getMaxPlayCount() != null) {
            if (criteria.getMinPlayCount() == null) {
                predicates.add(cb.or(cb.isNull(mediaFile.get("playCount")),
                        cb.lessThanOrEqualTo(mediaFile.get("playCount"), criteria.getMaxPlayCount())));
            } else {
                predicates.add(cb.lessThanOrEqualTo(mediaFile.get("playCount"), criteria.getMaxPlayCount()));
            }
        }
    }

}
