package com.br.mamba_wedding.events.infrastructure;

import com.br.mamba_wedding.events.domain.EventInvitation;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.guests.domain.GuestSide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventInvitationRepository extends JpaRepository<EventInvitation, Long> {

    @EntityGraph(attributePaths = {"event", "guest"})
    Optional<EventInvitation> findByEventIdAndGuestId(Long eventId, Long guestId);

    @EntityGraph(attributePaths = {"event", "guest"})
    List<EventInvitation> findAllByGuestIdOrderByEventIdAsc(Long guestId);

    @EntityGraph(attributePaths = {"event", "guest"})
    @Query(
            value = """
                    select invitation
                    from EventInvitation invitation
                    join invitation.guest guest
                    where invitation.event.id = :eventId
                      and (:name is null or lower(guest.fullName) like lower(concat('%', :name, '%')))
                      and (:status is null or invitation.rsvpStatus = :status)
                      and (:side is null or guest.side = :side)
                    order by lower(guest.fullName), guest.id
                    """,
            countQuery = """
                    select count(invitation)
                    from EventInvitation invitation
                    join invitation.guest guest
                    where invitation.event.id = :eventId
                      and (:name is null or lower(guest.fullName) like lower(concat('%', :name, '%')))
                      and (:status is null or invitation.rsvpStatus = :status)
                      and (:side is null or guest.side = :side)
                    """
    )
    Page<EventInvitation> search(
            @Param("eventId") Long eventId,
            @Param("name") String name,
            @Param("status") RsvpStatus status,
            @Param("side") GuestSide side,
            Pageable pageable
    );

    @Query("""
            select invitation.rsvpStatus as status, count(invitation) as total
            from EventInvitation invitation
            where invitation.event.id = :eventId
            group by invitation.rsvpStatus
            """)
    List<RsvpStatusCount> countByStatusForEvent(@Param("eventId") Long eventId);
}
