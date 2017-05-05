package basics

import faunadb.FaunaClient
import faunadb.query._
import faunadb.values._
import utils.FaunaUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

object FaunaBasics {


  def main(args: Array[String]): Unit = {

    val DB_NAME = "my_app"
    runBasiscs(rootClient, DB_NAME)
  }

  //The basic tutorial w/ out any proper error handling
  def runBasiscs(rootClient: FaunaClient, databaseName: String): Unit = {

    //drop the database
    ready(rootClient.query(Delete(Database(databaseName))))

    //create a new database and indexes
    val databaseClient = setupDB(rootClient, databaseName)

    //populate the database and return the list of post refs
    val postRefs = populatePosts(databaseClient)

    //query for posts
    retrievePosts(databaseClient, postRefs)

    //modify the posts
    modifyPosts(databaseClient, postRefs)

    //delete the last post
    val updatedPostsRefs = deletePost(databaseClient, postRefs)

    //iterate over the posts
    iterateOverPosts(databaseClient, updatedPostsRefs)
  }

  def setupDB(rootClient: FaunaClient, databaseName: String): FaunaClient = {

    val db = await(rootClient.query(CreateDatabase(Obj("name" -> databaseName))))
    val dbRef = db(RefField).get
    val key = await(rootClient.query(CreateKey(Obj("database" -> dbRef, "role" -> "server"))))
    val databaseKey = key(SecretField).get
    val databaseClient = FaunaClient(databaseKey)
    await(databaseClient.query(CreateClass(Obj("name" -> "posts"))))

    val i1 = databaseClient.query(
      CreateIndex(
        Obj(
          "name" -> "posts_by_title",
          "source" -> Class("posts"),
          "terms" -> Arr(Obj("field" -> Arr("data", "title")))
        )))

    val i2 = databaseClient.query(
      CreateIndex(
        Obj(
          "name" -> "posts_by_tags_with_title",
          "source" -> Class("posts"),
          "terms" -> Arr(Obj("field" -> Arr("data", "tags"))),
          "values" -> Arr(Obj("field" -> Arr("data", "title")))
        )))

    ready(Future.sequence(Seq(i1, i2)))

    databaseClient
  }

  def populatePosts(client: FaunaClient): Seq[String] = {
    val firstPost = client.query(
      Create(
        Class("posts"),
        Obj("data" -> Obj("title" -> "What I had for breakfast .."))))

    results("first post:", firstPost)


    val postsFuture = client.query(
      Map(
        Arr(
          "My cat and other marvels",
          "Pondering during a commute",
          "Deep meanings in a latte"
        ),
        Lambda { post_title =>
          Create(
            Class("posts"),
            Obj("data" -> Obj("title" -> post_title)))
        }))

    //get the posts out without proper error handling - should use things like get or else ...
    val listPostRefs = postsFuture.map(posts => {
      val refs = posts.collect(RefField).get
      refs.map(_.value)
    })

    val eventualPostRefs = await(listPostRefs)
    println(
      s"post refs: ${eventualPostRefs} "
    )

    eventualPostRefs
  }

  def retrievePosts(client: FaunaClient, postRefs: Seq[String]): Unit = {

    //hope we have a list of posts and get first one
    val postByRef = client.query(Get(Ref(postRefs.head)))
    results("first post by ref", postByRef)

    val posts = client.query(
      Get(
        Match(Index("posts_by_title"), "Deep meanings in a latte")))
    results("post by title:", posts)

  }

  def modifyPosts(client: FaunaClient, postRefs: Seq[String]): Unit = {
    val firstPost = postRefs.head

    val u1 = client.query(
      Update(
        Ref(firstPost),
        Obj("data" -> Obj("tags" -> Arr("pet", "cute")))))

    results("updated post:", u1)

    val u2 = client.query(
      Replace(
        Ref(firstPost),
        Obj("data" -> Obj("title" -> "My dog and other marvels"))))

    results("next updated post:", u2)
  }

  def deletePost(client: FaunaClient, postRefs: Seq[String]): Seq[String] = {
    val deleteRef = client.query(Delete(Ref(postRefs.last)))
    results("deleted:", deleteRef)

    val checkPost = client.query(Get(Ref(postRefs.last)))
    results("check post:", checkPost)

    postRefs.dropRight(1)
  }

  def iterateOverPosts(client: FaunaClient, postRefs: Seq[String]): Unit = {

    //wild assumption that the second post we created is still the one about commuting ...
    val updateTags = client.query(
      Do(
        Update(
          Ref(postRefs.head),
          Obj("data" -> Obj("tags" -> Arr("pet", "cute", "funny")))),
        Update(
          Ref(postRefs(1)),
          Obj("data" -> Obj("tags" -> Arr("philosophy", "travel")))),
        Create(
          Class("posts"),
          Obj(
            "data" -> Obj(
              "title" -> "Homebound",
              "tags" -> Arr("nostalgia", "travel")
            )
          )),
        Create(
          Class("posts"),
          Obj(
            "data" -> Obj(
              "title" -> "All Aboard",
              "tags" -> Arr("ship", "travel")
            )
          ))))

    results("last updated post:", updateTags)

    val postsByTravelTagFut = client.query(
      Paginate(
        Match(Index("posts_by_tags_with_title"), "travel"),
        size = 2))


    val postsByTravelTag = await(postsByTravelTagFut)
    println(s"postsByTravelTag: $postsByTravelTag")

    val nextSetPosts = client.query(
      Paginate(
        Match(Index("posts_by_tags_with_title"), "travel"),
        After(postsByTravelTag("after")),
        size = 2))

    results("next set posts:", nextSetPosts)


    val postTitles = client.query(
      Map(
        Paginate(Match(Index("posts_by_tags_with_title"), "travel")),
        Lambda { post => Casefold(post) }))

    results("post titles on travel:", postTitles)
  }
}
