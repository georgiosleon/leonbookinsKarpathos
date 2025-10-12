package com.leonsoft.research;

import com.leonsoft.research.basic.string.StringDataField;
import com.leonsoft.research.basic.string.StringDataFieldConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Event {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    @Convert(converter = StringDataFieldConverter.class)
    private StringDataField name  = new StringDataField();

    @Column(name = "notes")
    @Convert(converter = StringDataFieldConverter.class)
    private StringDataField notes  = new StringDataField();

//    @Column(name = "email")
//    private EmailStringDataField email   = new EmailStringDataField();

//    @Column(name = "start_date")
//    private LocalDateDataField startDate   = new LocalDateDataField();
//
//    @Column(name = "end_date")
//    private LocalDateDataField endDate   = new LocalDateDataField();


    //HashMap<String, Object>  data = new HashMap<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass =
              o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                    : o.getClass();
        Class<?> thisEffectiveClass =
              this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                    : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        Event event = (Event) o;
        return getId() != null && Objects.equals(getId(), event.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
