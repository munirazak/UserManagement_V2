package com.kopibru.usermanagement.repository;

import com.kopibru.usermanagement.model.Borrower;
import com.kopibru.usermanagement.model.BorrowerRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, Long> {

    boolean existsByLibraryId(String libraryId);

    boolean existsByRole(BorrowerRole role);

    Optional<Borrower> findByLibraryId(String libraryId);
}
