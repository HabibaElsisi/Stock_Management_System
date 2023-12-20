import ProductActor.{AddProduct, DeleteProduct, ProductList}
import ProductApp.productActor
import akka.actor.typed.ActorRefResolver.id
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.io.Codec.fallbackSystemCodec.name
import scala.io.StdIn
object ProductActor {
  case class ViewSpecificProduct(productId: Int)

  case class LowQuantityAlert(productId: Int)
  case class RetrieveProduct(productId: Int)
  case class UpdateProduct(product: Product)
  case class AddProduct(product: Product)
  case class DeleteProduct(productId: Int)
  case object ShowProduct
  case class ProductList(products: List[Product])
























  def props(filePath: String): Props =
    Props(new ProductActor(filePath))
}
class ProductActor(filePath: String) extends Actor {

  import ProductActor._

  var products: List[Product] = readProductFromFile()

  override def receive: Receive = {
    case AddProduct(product) =>
      products = product :: products
      writeProductsToFile(products)
    case DeleteProduct(productId) =>
      products = products.filterNot(_.id == productId)
      writeProductsToFile(products)
    case ShowProduct =>
      sender() ! ProductList(products)

    case UpdateProduct(updatedProduct) =>
      products = products.map {
        case existingProduct if existingProduct.id == updatedProduct.id =>
          // Update the existing doctor with new details
          updatedProduct
        case otherDoctor =>
          otherDoctor
      }
      writeProductsToFile(products)
    case ViewSpecificProduct(productId) =>
      val productOpt = products.find(_.id == productId)
      sender() ! productOpt


    case ViewSpecificProduct(productId) =>
      val productOpt = products.find(_.id == productId)
      sender() ! productOpt


    case LowQuantityAlert(productId) =>
      val productOpt = products.find(_.id == productId)
      productOpt.foreach { product =>
        if (product.quantity.toInt <= 20) {
          println(s"Warning: Product ${product.name} has low quantity (${product.quantity}).")
          println("Enter the additional quantity to be added:")
          try {
            val additionalQuantity = StdIn.readInt()
            val updatedQuantity = product.quantity.toInt + additionalQuantity
            val updatedProduct = product.copy(quantity = updatedQuantity.toString)
            products = products.map {
              case p if p.id == productId => updatedProduct
              case otherProduct => otherProduct
            }
            writeProductsToFile(products)
          } catch {
            case e: NumberFormatException =>
              println("Invalid input for quantity. Please enter a valid number.")
          }
        }
      }
  }
  private def readProductFromFile(): List[Product] = {
    if (Files.exists(Paths.get(filePath))) {
      val lines = Files.readAllLines(Paths.get(filePath))
      lines.toArray(Array.ofDim[String](lines.size())).flatMap { line =>
        if (line.nonEmpty) {
          line.split(",") match {
            case Array(id, name, quantity, price, supplier) => Some(Product(id.toInt, name, quantity, price, supplier))
            case _ => None
          }
        } else {
          None
        }
      }.toList
    } else {
      List()
    }
  }

  private def writeProductsToFile(products: List[Product]): Unit = {
    val productStrings = products.map(product => s"${product.id},${product.name},${product.quantity},${product.price},${product.supplier}")
    Files.write(Paths.get(filePath), productStrings.mkString("\n").getBytes, StandardOpenOption.CREATE)
  }
}

case class Product(id: Int, name: String, quantity: String, price: String, supplier: String)

object ProductApp extends App {
  val filePath = "src/main/resources/temp_inventory.txt" // Adjust the path as needed
  val system: ActorSystem = ActorSystem("ProductActorSystem")
  val productActor: ActorRef = system.actorOf(ProductActor.props(filePath), "productActor")
  implicit val timeout: Timeout = Timeout(5.seconds)
  while (true) {
    println("1. Add Product")
    println("2. Delete Product")
    println("3. Show Product")
    println("4. Update Product")
    println("5.  view specific product")
    println("0. Exit")
    print("Enter your choice: ")
    val choice = StdIn.readInt()
    choice match {
      case 1 =>
        println("Enter product details:")
        print("Product ID: ")
        val id = StdIn.readInt()
        print("Product Name: ")
        val name = StdIn.readLine()
        print("Product quantity: ")
        val quantity = StdIn.readLine()
        print("Product price ")
        val price = StdIn.readLine()
        print("Product supplier ")
        val supplier = StdIn.readLine()
        productActor ! ProductActor.AddProduct(Product(id, name, quantity, price, supplier))
      case 2 =>
        println("Enter product ID to delete:")
        val productId = StdIn.readInt()
        productActor ! ProductActor.DeleteProduct(productId)
      case 3 =>
        val productListFuture = (productActor ? ProductActor.ShowProduct).mapTo[ProductActor.ProductList]
        val productList = Await.result(productListFuture, timeout.duration)
        println("Prpducts:")
        productList.products.foreach(product => println(s"${product.id}, ${product.name} , ${product.quantity},${product.price},${product.supplier}"))
      case 4 =>
        println("Enter product details for update:")
        print("Product ID: ")
        val updateId = StdIn.readInt()
        print("Product Name: ")
        val updateName = StdIn.readLine()
        print("Product Quantity: ")
        val updatequantity = StdIn.readLine()
        print("Product price: ")
        val updateprice = StdIn.readLine()
        print("Product supplier: ")
        val updatesupplier = StdIn.readLine()
        productActor ! ProductActor.UpdateProduct(Product(updateId, updateName, updatequantity, updateprice, updatesupplier))
      case 5 =>
        println("Enter product ID to view:")
        val productId = StdIn.readInt()
        productActor ! ProductActor.ViewSpecificProduct(productId)
        val productOptFuture = (productActor ? ProductActor.ViewSpecificProduct(productId)).mapTo[Option[Product]]
        val productOpt = Await.result(productOptFuture, timeout.duration)
        productOpt match {
          case Some(product) =>
            println(s"Product details: ${product.id}, ${product.name}, ${product.quantity}, ${product.price}, ${product.supplier}")
            productActor ! ProductActor.LowQuantityAlert(productId)
          case None =>
            println("Product not found.")
        }

      case 0 =>
        system.terminate()
        System.exit(0)

      case _ =>
        println("Invalid choice. Please enter a valid option.")


    }

    }

}