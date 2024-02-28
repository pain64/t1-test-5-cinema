package org.example

import java.math.BigDecimal
import java.util.*

data class Provider(
    val id: Long,
    val companyName: String,
    val sub: List<Provider>
)

data class Movie(
    val id: Long,
    val name: String,
    val description: String
)

class Reservation(
    val client: Client,
    val session: Session,
    val seats: List<Int>,
)

data class Session(
    val id: Long, val movie: Movie,
    val seatCount: Int, val date: Date,
    val provider: Provider, val providerEarn: BigDecimal
)

data class Client(val id: Long, val name: String)

sealed interface ReserveResult
data object ReserveOk : ReserveResult
data class SeatAlreadyReserved(val seat: Int) : ReserveResult

class ClientNotFoundException(val clientId: Long) : Exception()
class SessionNotFoundException(val sessionId: Long) : Exception()
class SeatNotFoundException(val seat: Int) : Exception()


class Cinema(
    private val rootProvider: Provider,
    private val movies: List<Movie>,
    private val clients: List<Client>,
    private val sessions: List<Session>,
    initialReservations: List<Reservation> = listOf()
) {
    private val reservations = initialReservations.toMutableList()

    /*
      Осуществляет бронирование с проверкой уже забронированных мест
     */
    fun reserve(clientId: Long, sessionId: Long, seats: List<Int>): ReserveResult {
        val client = clients.find { it.id == clientId }
            ?: throw ClientNotFoundException(clientId)
        val session = sessions.find { it.id == sessionId }
            ?: throw SessionNotFoundException(sessionId)

        val unknownSeat = seats.find { it > session.seatCount }
        if (unknownSeat != null) throw SeatNotFoundException(unknownSeat)

        val reservedSeats = reservations
            .filter { it.session == session }
            .fold(emptyList<Int>()) { l, r -> l + r.seats }

        val reservedSeat = seats.find { it in reservedSeats }
        return if (reservedSeat != null)
            SeatAlreadyReserved(reservedSeat)
        else {
            reservations.add(Reservation(client, session, seats))
            ReserveOk
        }
    }

    /*
      Возвращает список фильмов, просмотренных пользователем, в соответствии
      с порядом показа фильмов
     */
    fun watchedMovies(clientId: Long): List<Movie> {
        val client = clients.find { it.id == clientId }
            ?: throw ClientNotFoundException(clientId)

        return reservations
            .filter { it.client == client }
            .map { it.session.movie }
    }

    /*
      Возвращает сумму, заработанную компанией-провайдером и его дочерними компаниями,
      в указанный диапазон дат
     */
    fun providerEarnings(provider: Provider, dateFrom: Date, dateTo: Date): BigDecimal {
        fun concreteProviderEarnings(p: Provider): BigDecimal =
            reservations.filter { it.session.provider == p }
                .sumOf { it.session.providerEarn }

        fun traverseAndAdd(p: Provider): BigDecimal =
            concreteProviderEarnings(p) + p.sub.sumOf {
                concreteProviderEarnings(it)
            }

        return traverseAndAdd(provider)
    }
}
