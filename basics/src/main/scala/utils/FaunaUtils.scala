package utils

import faunadb.FaunaClient
import faunadb.values.{Field, RefV, Value}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object FaunaUtils {

  val rootKey = Option(System.getenv("FAUNA_ROOT_KEY")) getOrElse {
    throw new RuntimeException("FAUNA_ROOT_KEY must defined to run tests")
  }

  val rootClient = FaunaClient(secret = rootKey)

  // Helper fields

  val RefField = Field("ref").to[RefV]
  val TsField = Field("ts").to[Long]
  val ClassField = Field("class").to[RefV]
  val SecretField = Field("secret").to[String]

  // Page helpers
  case class Ev(ref: RefV, ts: Long, action: String)

  val EventField = Field.zip(
    Field("resource").to[RefV],
    Field("ts").to[Long],
    Field("action").to[String]
  ) map { case (r, ts, a) => Ev(r, ts, a) }

  val PageEvents = Field("data").collect(EventField)
  val PageRefs = Field("data").to[Seq[RefV]]

  def await[T](f: Future[T]) = Await.result(f, 5.second)

  def ready[T](f: Future[T]) = Await.ready(f, 5.second)

  def results(msg: String, futureValue: Future[Value]) = {
    println(
      s" $msg ${ready(futureValue)} "
    )
  }

}
