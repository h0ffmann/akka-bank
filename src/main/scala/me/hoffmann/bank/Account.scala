/*
 * Copyright 2020 Matheus Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.hoffmann.bank

import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.scaladsl.{ EntityContext, EntityTypeKey }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior, ReplyEffect }
import akka.persistence.typed.PersistenceId
import java.io.{ Serializable => JavaSerializable }

/**
  * Persistent actor maintaining a balance and receiving [[Account.Deposit]] and
  * [[Account.Withdraw]] commands such that the balance never becomes negative. The amount to be
  *  deposited or withdrawn must be positive.
  */
object Account {

  sealed trait Command
  sealed trait Event
  sealed trait Serializable extends JavaSerializable

  // No `GetBalance` command, because this is the write side only ('C' in CQRS)!

  final case class Deposit(amount: Int, replyTo: ActorRef[DepositReply])
      extends Command
      with Serializable

  sealed trait DepositReply

  final case class Deposited(amount: Int) extends Event with DepositReply with Serializable

  final case class Withdraw(amount: Int, replyTo: ActorRef[WithdrawReply])
      extends Command
      with Serializable

  sealed trait WithdrawReply

  final case class InsufficientBalance(amount: Int, balance: Long)
      extends WithdrawReply
      with Serializable

  final case class Withdrawn(amount: Int) extends Event with WithdrawReply with Serializable

  final case class InvalidAmount(amount: Int)
      extends DepositReply
      with WithdrawReply
      with Serializable

  final case class State(balance: Long)

  final val Name = "account"

  val typeKey: EntityTypeKey[Command] =
    EntityTypeKey(Name)

  private val commandHandler: (State, Command) => ReplyEffect[Event, State] = {
    case (_, Deposit(amount, replyTo)) if amount <= 0 =>
      Effect.reply(replyTo)(InvalidAmount(amount))

    case (_, Deposit(amount, replyTo)) =>
      val deposited = Deposited(amount)
      Effect
        .persist(deposited)
        .thenReply(replyTo)(_ => deposited)

    case (_, Withdraw(amount, replyTo)) if amount <= 0 =>
      Effect.reply(replyTo)(InvalidAmount(amount))

    case (State(balance), Withdraw(amount, replyTo)) if balance < amount =>
      Effect.reply(replyTo)(InsufficientBalance(amount, balance))

    case (_, Withdraw(amount, replyTo)) =>
      val withdrawn = Withdrawn(amount)
      Effect
        .persist(withdrawn)
        .thenReply(replyTo)(_ => withdrawn)
  }

  private val eventHandler: (State, Event) => State = {
    case (State(balance), Deposited(amount)) => State(balance + amount)
    case (State(balance), Withdrawn(amount)) => State(balance - amount)
  }

  def apply(context: EntityContext[Account.Command]): Behavior[Command] =
    Account(PersistenceId(context.entityTypeKey.name, context.entityId))

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies(
        persistenceId,
        State(0),
        commandHandler,
        eventHandler
      )
      .withTagger(_ => Set(Name))

  /**
    * Helper for asking.
    */
  def deposit(amount: Int)(replyTo: ActorRef[DepositReply]): Deposit =
    Deposit(amount, replyTo)

  /**
    * Helper for asking.
    */
  def withdraw(amount: Int)(replyTo: ActorRef[WithdrawReply]): Withdraw =
    Withdraw(amount, replyTo)
}
